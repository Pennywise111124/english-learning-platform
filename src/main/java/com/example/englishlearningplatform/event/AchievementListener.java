package com.example.englishlearningplatform.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.englishlearningplatform.service.AchievementService;

@Component
public class AchievementListener {

    private static final Logger log = LoggerFactory.getLogger(AchievementListener.class);

    private final AchievementService achievementService;

    public AchievementListener(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onXpAwarded(XpAwardedEvent e) {
        try {
            achievementService.evaluate(e.userId());
        } catch (Exception ex) {
            log.error("Failed to evaluate achievements for user {}", e.userId(), ex);
        }
    }
}