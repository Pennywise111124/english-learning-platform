package com.example.englishlearningplatform.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

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

import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.event.GamificationListener;
import com.example.englishlearningplatform.event.LearningActivityEvent;
import com.example.englishlearningplatform.repository.PostgresIntegrationTestBase;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.repository.UserStatsRepository;
import com.example.englishlearningplatform.repository.XpEventRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ GamificationService.class, GamificationListener.class, GamificationTestConfig.class })
@TestPropertySource(properties = {
        "app.gamification.xp-quiz-participation=10",
        "app.gamification.xp-quiz-pass=10",
        "app.gamification.xp-vocab-review=2",
        "app.gamification.xp-dictation-max=10",
        "app.quiz.pass-score=70",
        "app.gamification.quiz-participation-daily-limit=3" })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class GamificationEventFlowTest extends PostgresIntegrationTestBase {

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

    private Long userId;
    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);

        User u = new User();
        u.setUsername("ef-" + UUID.randomUUID().toString().substring(0, 8));
        u.setEmail(UUID.randomUUID() + "@test.local");
        u.setPasswordHash("hash");
        userId = userRepo.save(u).getId();
    }

    @AfterEach
    void cleanUp() {
        xpRepo.deleteAll(xpRepo.findByUserId(userId));
        statsRepo.deleteById(userId);
        userRepo.deleteById(userId);
    }

    @Test
    void eventPublishedInCommittedTransaction_awardsXp() {
        tx.executeWithoutResult(s -> publisher.publishEvent(
                LearningActivityEvent.vocabReviewed(userId, 1L, true, Instant.parse("2026-10-09T03:00:00Z"))));

        assertThat(statsRepo.findById(userId).orElseThrow().getTotalXp()).isEqualTo(2);
        assertThat(xpRepo.findByUserId(userId)).hasSize(1);
    }

    @Test
    void eventPublishedInRolledBackTransaction_awardsNothing() {
        tx.executeWithoutResult(s -> {
            publisher.publishEvent(
                    LearningActivityEvent.vocabReviewed(userId, 1L, true, Instant.parse("2026-10-09T03:00:00Z")));
            s.setRollbackOnly();
        });

        assertThat(statsRepo.findById(userId)).isEmpty();
        assertThat(xpRepo.findByUserId(userId)).isEmpty();
    }
}