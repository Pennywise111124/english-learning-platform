package com.example.englishlearningplatform.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

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
    @Mock
    private Cache flashcardsByTopicCache;

    @InjectMocks
    private TopicCacheEvictionListener listener;

    @BeforeEach
    void stubFlashcardsCacheLeniently() {
        lenient().when(cacheManager.getCache("flashcardsByTopic")).thenReturn(flashcardsByTopicCache);
    }

    @Test
    void onTopicChanged_withNullTopicId_shouldClearTopicsCacheOnlyAndNeverTouchPerTopicCaches() {
        when(cacheManager.getCache("topics")).thenReturn(topicsCache);
        TopicChangedEvent event = new TopicChangedEvent(null);

        listener.onTopicChanged(event);

        verify(topicsCache).clear();
        verify(cacheManager, never()).getCache("topicDetails");
        verify(cacheManager, never()).getCache("flashcardsByTopic");
        verifyNoInteractions(flashcardsByTopicCache);
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

    @Test
    void onTopicChanged_withTopicId_evictsFlashcardsByTopicWithThatKey() {
        when(cacheManager.getCache("topics")).thenReturn(topicsCache);
        when(cacheManager.getCache("topicDetails")).thenReturn(topicDetailsCache);

        listener.onTopicChanged(new TopicChangedEvent(5L));

        verify(flashcardsByTopicCache).evict(5L);
    }

    @Test
    void onTopicChanged_whenTopicDetailsCacheIsNull_stillEvictsFlashcardsByTopic() {
        when(cacheManager.getCache("topics")).thenReturn(topicsCache);
        when(cacheManager.getCache("topicDetails")).thenReturn(null);

        listener.onTopicChanged(new TopicChangedEvent(5L));

        verify(flashcardsByTopicCache).evict(5L);
    }

    @Test
    void onTopicChanged_whenFlashcardsCacheNotConfigured_shouldNotThrow() {
        when(cacheManager.getCache("topics")).thenReturn(topicsCache);
        when(cacheManager.getCache("topicDetails")).thenReturn(topicDetailsCache);
        when(cacheManager.getCache("flashcardsByTopic")).thenReturn(null);

        assertDoesNotThrow(() -> listener.onTopicChanged(new TopicChangedEvent(5L)));
    }

    @Test
    void onTopicChanged_isRegisteredToRunAfterCommit() throws NoSuchMethodException {
        Method method = TopicCacheEvictionListener.class.getMethod("onTopicChanged", TopicChangedEvent.class);
        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);

        assertNotNull(annotation);
        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
    }
}