package com.example.englishlearningplatform.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.englishlearningplatform.service.GamificationService;

@ExtendWith(MockitoExtension.class)
class GamificationListenerTest {

    @Mock
    private GamificationService gamificationService;
    @InjectMocks
    private GamificationListener listener;

    private final LearningActivityEvent event = LearningActivityEvent.vocabReviewed(
            1L, 2L, true, Instant.parse("2026-10-09T03:00:00Z"));

    @Test
    void onLearningActivity_delegatesToService() {
        listener.onLearningActivity(event);

        verify(gamificationService).record(event);
    }

    @Test
    void onLearningActivity_whenServiceFails_swallowsException() {
        doThrow(new IllegalStateException("boom")).when(gamificationService).record(event);

        assertDoesNotThrow(() -> listener.onLearningActivity(event));
    }
}