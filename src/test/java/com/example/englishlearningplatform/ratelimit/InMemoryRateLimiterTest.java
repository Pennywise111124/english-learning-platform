package com.example.englishlearningplatform.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

    static class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-10-12T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MutableClock clock;
    private InMemoryRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        limiter = new InMemoryRateLimiter(clock);
    }

    @Test
    void limitThree_firstThreeAllowed_fourthDeniedWithRetryAfterInsideWindow() {
        Duration window = Duration.ofSeconds(60);
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("k", 3, window).allowed()).isTrue();
        }

        RateLimitDecision fourth = limiter.tryAcquire("k", 3, window);

        assertThat(fourth.allowed()).isFalse();
        assertThat(fourth.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    void differentKeys_doNotAffectEachOther() {
        limiter.tryAcquire("a", 1, Duration.ofSeconds(60));
        assertThat(limiter.tryAcquire("a", 1, Duration.ofSeconds(60)).allowed()).isFalse();

        assertThat(limiter.tryAcquire("b", 1, Duration.ofSeconds(60)).allowed()).isTrue();
    }

    @Test
    void afterWindowExpires_counterResets() {
        Duration window = Duration.ofSeconds(10);
        limiter.tryAcquire("k", 1, window);
        assertThat(limiter.tryAcquire("k", 1, window).allowed()).isFalse();

        clock.advance(Duration.ofSeconds(10));

        assertThat(limiter.tryAcquire("k", 1, window).allowed()).isTrue();
    }

    @Test
    void stillInsideWindow_oneSecondBeforeExpiry_isStillDenied() {
        Duration window = Duration.ofSeconds(10);
        limiter.tryAcquire("k", 1, window);

        clock.advance(Duration.ofSeconds(9));

        assertThat(limiter.tryAcquire("k", 1, window).allowed()).isFalse();
    }

    @Test
    void retryAfter_isRemainingTimeOfWindow() {
        Duration window = Duration.ofSeconds(10);
        limiter.tryAcquire("k", 1, window);
        clock.advance(Duration.ofSeconds(4));

        RateLimitDecision denied = limiter.tryAcquire("k", 1, window);

        assertThat(denied.retryAfterSeconds()).isEqualTo(6L);
    }

    @Test
    void retryAfter_isNeverBelowOne() {
        Duration window = Duration.ofSeconds(10);
        limiter.tryAcquire("k", 1, window);
        clock.advance(Duration.ofMillis(9_999));

        assertThat(limiter.tryAcquire("k", 1, window).retryAfterSeconds()).isEqualTo(1L);
    }

    @Test
    void fiftyThreadsWithLimitTen_exactlyTenAllowed() throws Exception {
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    if (limiter.tryAcquire("k", 10, Duration.ofSeconds(60)).allowed()) {
                        allowed.incrementAndGet();
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
    }

    @Test
    void invalidArguments_areRejected() {
        assertThatThrownBy(() -> limiter.tryAcquire("k", 0, Duration.ofSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limiter.tryAcquire("k", 5, Duration.ofMillis(500)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}