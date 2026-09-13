package com.example.englishlearningplatform.event;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FlashcardCacheEvictionListener {

    private final CacheManager cacheManager;

    public FlashcardCacheEvictionListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFlashcardChanged(FlashcardChangedEvent event) {
        Cache flashcardsCache = cacheManager.getCache("flashcardsByTopic");
        if (flashcardsCache != null) {
            flashcardsCache.evict(event.getTopicId());
        }
    }
}