package com.example.englishlearningplatform.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.englishlearningplatform.service.AchievementService;

@ExtendWith(MockitoExtension.class)
class AchievementListenerTest {

    @Mock
    AchievementService achievementService;

    @InjectMocks
    AchievementListener listener;

    @Test
    void onXpAwarded_delegatesToEvaluate() {
        listener.onXpAwarded(new XpAwardedEvent(7L));

        verify(achievementService).evaluate(7L);
    }

    @Test
    void evaluateThrowing_isSwallowed() {
        doThrow(new RuntimeException("boom")).when(achievementService).evaluate(7L);

        assertDoesNotThrow(() -> listener.onXpAwarded(new XpAwardedEvent(7L)));
    }
}