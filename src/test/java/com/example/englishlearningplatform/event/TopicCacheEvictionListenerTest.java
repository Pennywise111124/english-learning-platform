package com.example.englishlearningplatform.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicCacheEvictionListenerTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache topicsCache;
    @Mock
    private Cache topicDetailsCache;

    @InjectMocks
    private TopicCacheEvictionListener listener;

    @Test
    void onTopicChanged_withNullTopicId_shouldClearTopicsCacheOnlyAndNeverTouchTopicDetails() {
        when(cacheManager.getCache("topics")).thenReturn(topicsCache);
        TopicChangedEvent event = new TopicChangedEvent(null);

        listener.onTopicChanged(event);

        verify(topicsCache).clear();
        verify(cacheManager, never()).getCache("topicDetails");
    }

    @Test
    void onTopicChanged_withNonNullTopicId_shouldClearTopicsCacheAndEvictTopicDetails() {
        when(cacheManager.getCache("topics")).thenReturn(topicsCache);
        when(cacheManager.getCache("topicDetails")).thenReturn(topicDetailsCache);
        TopicChangedEvent event = new TopicChangedEvent(5L);

        listener.onTopicChanged(event);

        verify(topicsCache).clear();
        verify(topicDetailsCache).evict(5L);
    }

    @Test
    void onTopicChanged_whenTopicsCacheIsNull_shouldNotThrow() {
        when(cacheManager.getCache("topics")).thenReturn(null);
        TopicChangedEvent event = new TopicChangedEvent(null);

        assertDoesNotThrow(() -> listener.onTopicChanged(event));
    }

    @Test
    void onTopicChanged_whenTopicDetailsCacheIsNull_shouldNotThrow() {
        when(cacheManager.getCache("topics")).thenReturn(topicsCache);
        when(cacheManager.getCache("topicDetails")).thenReturn(null);
        TopicChangedEvent event = new TopicChangedEvent(5L);

        assertDoesNotThrow(() -> listener.onTopicChanged(event));
    }
}