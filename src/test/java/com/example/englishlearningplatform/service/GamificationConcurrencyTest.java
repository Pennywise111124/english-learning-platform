package com.example.englishlearningplatform.service;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.entity.UserStats;
import com.example.englishlearningplatform.event.LearningActivityEvent;
import com.example.englishlearningplatform.repository.PostgresIntegrationTestBase;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.repository.UserStatsRepository;
import com.example.englishlearningplatform.repository.XpEventRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ GamificationService.class, GamificationTestConfig.class })
@TestPropertySource(properties = {
        "app.gamification.xp-quiz-participation=10",
        "app.gamification.xp-quiz-pass=10",
        "app.gamification.xp-vocab-review=2",
        "app.gamification.xp-dictation-max=10",
        "app.gamification.quiz-pass-score=70",
        "app.gamification.quiz-participation-daily-limit=3" })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class GamificationConcurrencyTest extends PostgresIntegrationTestBase {

    private static final String MORNING_OCT_9 = "2026-10-09T03:00:00Z";

    @Autowired
    GamificationService service;
    @Autowired
    UserRepository userRepo;
    @Autowired
    UserStatsRepository statsRepo;
    @Autowired
    XpEventRepository xpRepo;
    @Autowired
    PlatformTransactionManager txManager;

    private TransactionTemplate tx;
    private ExecutorService executor;
    private Long userId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
        executor = Executors.newFixedThreadPool(2);

        User u = new User();
        u.setUsername("gc-" + UUID.randomUUID().toString().substring(0, 8));
        u.setEmail(UUID.randomUUID() + "@test.local");
        u.setPasswordHash("hash");
        userId = userRepo.save(u).getId();
    }

    @AfterEach
    void cleanUp() {
        executor.shutdownNow();
        xpRepo.deleteAll(xpRepo.findByUserId(userId));
        statsRepo.deleteById(userId);
        userRepo.deleteById(userId);
    }

    private LearningActivityEvent vocab(long id) {
        return LearningActivityEvent.vocabReviewed(userId, id, true, Instant.parse(MORNING_OCT_9));
    }

    /** Cho 2 luồng cùng xuất phát một lúc. */
    private void runTogether(LearningActivityEvent first, LearningActivityEvent second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        Future<?> f1 = executor.submit(() -> {
            ready.countDown();
            go.await();
            service.record(first);
            return null;
        });
        Future<?> f2 = executor.submit(() -> {
            ready.countDown();
            go.await();
            service.record(second);
            return null;
        });

        assertThat(ready.await(5, SECONDS)).isTrue();
        go.countDown();

        f1.get(10, SECONDS); // ném ExecutionException nếu có lỗi khoá, deadlock, vi phạm unique...
        f2.get(10, SECONDS);
    }

    @Test
    void twoThreads_newUser_differentEvents_sumXpAndStayOneStreakDay() throws Exception {
        runTogether(vocab(1L), vocab(2L));

        UserStats s = statsRepo.findById(userId).orElseThrow();
        assertThat(s.getTotalXp()).isEqualTo(4);
        assertThat(s.getCurrentStreak()).isEqualTo(1);
        assertThat(s.getLongestStreak()).isEqualTo(1);
        assertThat(xpRepo.findByUserId(userId)).hasSize(2);
    }

    @Test
    void twoThreads_sameEvent_awardsXpOnlyOnce() throws Exception {
        runTogether(vocab(1L), vocab(1L));

        UserStats s = statsRepo.findById(userId).orElseThrow();
        assertThat(s.getTotalXp()).isEqualTo(2);
        assertThat(s.getCurrentStreak()).isEqualTo(1);
        assertThat(xpRepo.findByUserId(userId)).hasSize(1);
    }

    /**
     * Test này mới là test bắt lỗi thiếu @Lock: luồng 1 giữ khoá dòng stats và sẽ
     * ghi total = 100.
     * record() của luồng 2 phải chờ, rồi cộng thêm trên số 100 (kết quả 102), không
     * đè mất.
     */
    @Test
    void record_waitsForStatsRowLock_andBuildsOnCommittedState() throws Exception {
        tx.executeWithoutResult(s -> statsRepo.insertIfAbsent(userId, Instant.now()));

        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<?> holder = executor.submit(() -> tx.executeWithoutResult(s -> {
            UserStats st = statsRepo.findByUserIdForUpdate(userId).orElseThrow();
            lockHeld.countDown();
            try {
                if (!release.await(10, SECONDS)) {
                    throw new IllegalStateException("release timeout");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            st.setTotalXp(100);
            st.setUpdatedAt(Instant.now());
        }));
        assertThat(lockHeld.await(5, SECONDS)).isTrue();

        Future<?> recorder = executor.submit(() -> service.record(vocab(1L)));

        // Đang bị chặn bởi khoá
        assertThatThrownBy(() -> recorder.get(1500, MILLISECONDS)).isInstanceOf(TimeoutException.class);

        release.countDown();
        holder.get(5, SECONDS);
        recorder.get(5, SECONDS);

        assertThat(statsRepo.findById(userId).orElseThrow().getTotalXp()).isEqualTo(102);
    }
}