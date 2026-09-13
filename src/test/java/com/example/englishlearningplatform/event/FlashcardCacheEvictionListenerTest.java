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
class FlashcardCacheEvictionListenerTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache flashcardsCache;

    @InjectMocks
    private FlashcardCacheEvictionListener listener;

    @Test
    void onFlashcardChanged_whenCacheExists_shouldEvictTopicId() {
        when(cacheManager.getCache("flashcardsByTopic")).thenReturn(flashcardsCache);
        FlashcardChangedEvent event = new FlashcardChangedEvent(10L);

        listener.onFlashcardChanged(event);

        verify(flashcardsCache).evict(10L);
    }

    @Test
    void onFlashcardChanged_whenCacheIsNull_shouldNotThrow() {
        when(cacheManager.getCache("flashcardsByTopic")).thenReturn(null);
        FlashcardChangedEvent event = new FlashcardChangedEvent(10L);

        assertDoesNotThrow(() -> listener.onFlashcardChanged(event));
    }
}