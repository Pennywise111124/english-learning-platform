package com.example.englishlearningplatform.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.englishlearningplatform.service.GamificationService;

@Component
public class GamificationListener {

    private static final Logger log = LoggerFactory.getLogger(GamificationListener.class);

    private final GamificationService gamificationService;

    public GamificationListener(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLearningActivity(LearningActivityEvent event) {
        try {
            gamificationService.record(event);
        } catch (Exception ex) {
            log.error("Failed to record XP for user {} kind {}", event.userId(), event.kind(), ex);
        }
    }
}