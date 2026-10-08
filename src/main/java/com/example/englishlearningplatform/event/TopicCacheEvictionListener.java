package com.example.englishlearningplatform.event;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TopicCacheEvictionListener {

    private final CacheManager cacheManager;

    public TopicCacheEvictionListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTopicChanged(TopicChangedEvent event) {
        Cache topicsCache = cacheManager.getCache("topics");
        if (topicsCache != null) {
            topicsCache.clear();
        }

        if (event.getTopicId() != null) {
            Cache topicDetailsCache = cacheManager.getCache("topicDetails");
            if (topicDetailsCache != null) {
                topicDetailsCache.evict(event.getTopicId());
            }

            Cache flashcardsCache = cacheManager.getCache("flashcardsByTopic");
            if (flashcardsCache != null) {
                flashcardsCache.evict(event.getTopicId());
            }
        }
    }
}