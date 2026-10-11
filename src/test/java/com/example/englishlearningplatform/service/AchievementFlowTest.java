package com.example.englishlearningplatform.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.englishlearningplatform.entity.AchievementCode;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.entity.UserAchievement;
import com.example.englishlearningplatform.entity.UserStats;
import com.example.englishlearningplatform.event.AchievementListener;
import com.example.englishlearningplatform.event.GamificationListener;
import com.example.englishlearningplatform.event.LearningActivityEvent;
import com.example.englishlearningplatform.event.XpAwardedEvent;
import com.example.englishlearningplatform.repository.PostgresIntegrationTestBase;
import com.example.englishlearningplatform.repository.UserAchievementRepository;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.repository.UserStatsRepository;
import com.example.englishlearningplatform.repository.XpEventRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ GamificationService.class, GamificationListener.class,
        AchievementService.class, AchievementListener.class, GamificationTestConfig.class })
@TestPropertySource(properties = {
        "app.gamification.xp-quiz-participation=10",
        "app.gamification.xp-quiz-pass=10",
        "app.gamification.xp-vocab-review=2",
        "app.gamification.xp-dictation-max=10",
        "app.quiz.pass-score=70",
        "app.gamification.quiz-participation-daily-limit=3" })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AchievementFlowTest extends PostgresIntegrationTestBase {

    private static final Instant EVENT_TIME = Instant.parse("2026-10-09T03:00:00Z");

    @Autowired
    ApplicationEventPublisher publisher;
    @Autowired
    PlatformTransactionManager txManager;
    @Autowired
    UserRepository userRepo;
    @Autowired
    UserStatsRepository statsRepo;
    @Autowired
    XpEventRepository xpRepo;
    @Autowired
    UserAchievementRepository achievementRepo;
    @Autowired
    AchievementService achievementService;

    private Long userId;
    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);

        User u = new User();
        u.setUsername("af-" + UUID.randomUUID().toString().substring(0, 8));
        u.setEmail(UUID.randomUUID() + "@test.local");
        u.setPasswordHash("hash");
        userId = userRepo.save(u).getId();
    }

    @AfterEach
    void cleanUp() {
        achievementRepo.deleteAll(achievementRepo.findByUserId(userId));
        xpRepo.deleteAll(xpRepo.findByUserId(userId));
        statsRepo.deleteById(userId);
        userRepo.deleteById(userId);
    }

    private void seedTotalXp(int xp) {
        tx.executeWithoutResult(s -> {
            statsRepo.insertIfAbsent(userId, Instant.now());
            UserStats stats = statsRepo.findByUserIdForUpdate(userId).orElseThrow();
            stats.setTotalXp(xp);
            statsRepo.save(stats);
        });
    }

    private void publishVocabReviewedInCommittedTx() {
        tx.executeWithoutResult(s -> publisher.publishEvent(
                LearningActivityEvent.vocabReviewed(userId, 1L, true, EVENT_TIME)));
    }

    @Test
    void eventPushingXpToThreshold_unlocksXp100() {
        seedTotalXp(98);

        publishVocabReviewedInCommittedTx();

        assertThat(statsRepo.findById(userId).orElseThrow().getTotalXp()).isEqualTo(100);
        assertThat(achievementRepo.findByUserId(userId))
                .extracting(UserAchievement::getCode)
                .containsExactly(AchievementCode.XP_100);
    }

    @Test
    void eventLeavingXpJustBelowThreshold_unlocksNothing() {
        seedTotalXp(97);

        publishVocabReviewedInCommittedTx();

        assertThat(statsRepo.findById(userId).orElseThrow().getTotalXp()).isEqualTo(99);
        assertThat(achievementRepo.findByUserId(userId)).isEmpty();
    }

    @Test
    void eventPublishedInRolledBackTransaction_unlocksNothing() {
        seedTotalXp(98);

        tx.executeWithoutResult(s -> {
            publisher.publishEvent(
                    LearningActivityEvent.vocabReviewed(userId, 1L, true, EVENT_TIME));
            s.setRollbackOnly();
        });

        assertThat(statsRepo.findById(userId).orElseThrow().getTotalXp()).isEqualTo(98);
        assertThat(achievementRepo.findByUserId(userId)).isEmpty();
    }

    @Test
    void twoThreadsEvaluateSameUser_producesExactlyOneRow() throws Exception {
        seedTotalXp(100);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    achievementService.evaluate(userId);
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

        assertThat(achievementRepo.findByUserId(userId))
                .extracting(UserAchievement::getCode)
                .containsExactly(AchievementCode.XP_100);
    }

    @Test
    void xpAwardedEventInRolledBackTransaction_unlocksNothing() {
        seedTotalXp(100);

        tx.executeWithoutResult(s -> {
            publisher.publishEvent(new XpAwardedEvent(userId));
            s.setRollbackOnly();
        });

        assertThat(achievementRepo.findByUserId(userId)).isEmpty();
    }

    @Test
    void xpAwardedEventInCommittedTransaction_unlocksXp100() {
        seedTotalXp(100);

        tx.executeWithoutResult(s -> publisher.publishEvent(new XpAwardedEvent(userId)));

        assertThat(achievementRepo.findByUserId(userId))
                .extracting(UserAchievement::getCode)
                .containsExactly(AchievementCode.XP_100);
    }
}