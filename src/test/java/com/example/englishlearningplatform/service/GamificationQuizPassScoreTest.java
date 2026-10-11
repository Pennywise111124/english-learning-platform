package com.example.englishlearningplatform.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.englishlearningplatform.config.GamificationProperties;
import com.example.englishlearningplatform.config.QuizProperties;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.entity.UserStats;
import com.example.englishlearningplatform.event.LearningActivityEvent;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.repository.UserStatsRepository;
import com.example.englishlearningplatform.repository.XpEventRepository;

@ExtendWith(MockitoExtension.class)
class GamificationQuizPassScoreTest {

    private static final long USER_ID = 1L;
    private static final long QUIZ_ID = 100L;
    private static final int PASS_SCORE = 85; // khác 70 có chủ đích

    @Mock
    UserRepository userRepository;
    @Mock
    UserStatsRepository userStatsRepository;
    @Mock
    XpEventRepository xpEventRepository;

    private GamificationService service;
    private final Instant now = Instant.parse("2026-10-11T03:00:00Z");

    @BeforeEach
    void setUp() {
        service = new GamificationService(userRepository, userStatsRepository, xpEventRepository,
                new GamificationProperties(10, 10, 2, 10, 3),
                new QuizProperties(PASS_SCORE),
                Clock.fixed(now, ZoneOffset.UTC));

        User user = new User();
        user.setId(USER_ID);
        user.setTimezone("Asia/Ho_Chi_Minh");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userStatsRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(new UserStats()));
        when(xpEventRepository.insertIfAbsent(any(), any(), anyInt(), any(), any(), any())).thenReturn(1);
    }

    @Test
    void scoreBelowConfiguredPassScore_doesNotGrantQuizPass() {
        service.record(LearningActivityEvent.quizSubmitted(USER_ID, QUIZ_ID, PASS_SCORE - 1, now));

        verify(xpEventRepository, never())
                .insertIfAbsent(any(), eq("QUIZ_PASS"), anyInt(), any(), any(), any());
    }

    @Test
    void scoreEqualToConfiguredPassScore_grantsQuizPass() {
        service.record(LearningActivityEvent.quizSubmitted(USER_ID, QUIZ_ID, PASS_SCORE, now));

        verify(xpEventRepository)
                .insertIfAbsent(eq(USER_ID), eq("QUIZ_PASS"), eq(10), any(), eq("QPASS:" + QUIZ_ID), any());
    }
}