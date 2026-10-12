package com.example.englishlearningplatform.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;

class RedisRateLimiterTest {

    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate template;
    private static RedisRateLimiter limiter;

    @BeforeAll
    static void start() {
        REDIS.start();
        factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        template = new StringRedisTemplate(factory);
        limiter = new RedisRateLimiter(template);
    }

    @AfterAll
    static void stop() {
        factory.destroy();
        REDIS.stop();
    }

    private static String key() {
        return "rl:test:" + UUID.randomUUID();
    }

    @Test
    void limitThree_firstThreeAllowed_fourthDeniedWithRetryAfterInsideWindow() {
        String key = key();
        Duration window = Duration.ofSeconds(60);

        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire(key, 3, window).allowed()).as("lần %d", i + 1).isTrue();
        }
        RateLimitDecision fourth = limiter.tryAcquire(key, 3, window);

        assertThat(fourth.allowed()).isFalse();
        assertThat(fourth.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    void differentKeys_doNotAffectEachOther() {
        String a = key();
        String b = key();
        limiter.tryAcquire(a, 1, Duration.ofSeconds(60));
        assertThat(limiter.tryAcquire(a, 1, Duration.ofSeconds(60)).allowed()).isFalse();

        assertThat(limiter.tryAcquire(b, 1, Duration.ofSeconds(60)).allowed()).isTrue();
    }

    @Test
    void afterWindowExpires_requestsAreAllowedAgain() {
        String key = key();
        Duration window = Duration.ofSeconds(2);
        assertThat(limiter.tryAcquire(key, 1, window).allowed()).isTrue();
        assertThat(limiter.tryAcquire(key, 1, window).allowed()).isFalse();

        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(limiter.tryAcquire(key, 1, window).allowed()).isTrue());
    }

    @Test
    void firstCall_setsTtlOnKey() {
        String key = key();

        limiter.tryAcquire(key, 5, Duration.ofSeconds(60));

        Long ttl = template.getExpire(key);
        assertThat(ttl).isNotNull().isBetween(1L, 60L);
    }

    @Test
    void keyThatLostItsTtl_isHealedWhenLimitIsExceeded() {
        String key = key();
        Duration window = Duration.ofSeconds(60);
        limiter.tryAcquire(key, 2, window);
        limiter.tryAcquire(key, 2, window);
        template.persist(key);
        assertThat(template.getExpire(key)).isEqualTo(-1L);

        RateLimitDecision denied = limiter.tryAcquire(key, 2, window);

        assertThat(denied.allowed()).isFalse();
        assertThat(template.getExpire(key)).isBetween(1L, 60L);
    }

    @Test
    void fiftyThreadsWithLimitTen_exactlyTenAllowed() throws Exception {
        String key = key();
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger denied = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    if (limiter.tryAcquire(key, 10, Duration.ofSeconds(60)).allowed()) {
                        allowed.incrementAndGet();
                    } else {
                        denied.incrementAndGet();
                    }
                    return null;
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            go.countDown();
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(allowed.get()).isEqualTo(10);
        assertThat(denied.get()).isEqualTo(40);
    }

    @Test
    void retryAfter_reflectsRemainingTtl() {
        String key = key();
        Duration window = Duration.ofSeconds(100);
        limiter.tryAcquire(key, 1, window);
        template.expire(key, Duration.ofSeconds(7));

        RateLimitDecision denied = limiter.tryAcquire(key, 1, window);

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isBetween(6L, 7L);
    }

    @Test
    void invalidArguments_areRejected() {
        assertThatThrownBy(() -> limiter.tryAcquire(key(), 0, Duration.ofSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limiter.tryAcquire(key(), 5, Duration.ofMillis(500)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}