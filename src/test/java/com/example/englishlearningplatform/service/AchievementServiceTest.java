package com.example.englishlearningplatform.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.example.englishlearningplatform.config.QuizProperties;
import com.example.englishlearningplatform.entity.UserStats;
import com.example.englishlearningplatform.entity.VocabularyStatus;
import com.example.englishlearningplatform.repository.DictationResultRepository;
import com.example.englishlearningplatform.repository.QuizAttemptRepository;
import com.example.englishlearningplatform.repository.UserAchievementRepository;
import com.example.englishlearningplatform.repository.UserStatsRepository;
import com.example.englishlearningplatform.repository.UserVocabularyRepository;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PASS_SCORE = 85;
    private static final Instant NOW = Instant.parse("2026-10-11T03:00:00Z");

    @Mock
    UserStatsRepository userStatsRepository;
    @Mock
    QuizAttemptRepository quizAttemptRepository;
    @Mock
    UserVocabularyRepository userVocabularyRepository;
    @Mock
    DictationResultRepository dictationResultRepository;
    @Mock
    UserAchievementRepository userAchievementRepository;

    private AchievementService service;

    @BeforeEach
    void setUp() {
        service = new AchievementService(userStatsRepository, quizAttemptRepository,
                userVocabularyRepository, dictationResultRepository, userAchievementRepository,
                new QuizProperties(PASS_SCORE), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void givenStats(int totalXp, int longestStreak) {
        UserStats stats = new UserStats();
        stats.setTotalXp(totalXp);
        stats.setLongestStreak(longestStreak);
        when(userStatsRepository.findById(USER_ID)).thenReturn(Optional.of(stats));
    }

    @Test
    void evaluate_whenStatsMissing_insertsNothing() {
        when(userStatsRepository.findById(USER_ID)).thenReturn(Optional.empty());

        service.evaluate(USER_ID);

        verifyNoInteractions(userAchievementRepository);
    }

    @Test
    void evaluate_onlyXpReached_insertsExactlyXp100() {
        givenStats(100, 0);

        service.evaluate(USER_ID);

        verify(userAchievementRepository, times(1)).insertIfAbsent(any(), any(), any());
        verify(userAchievementRepository).insertIfAbsent(eq(USER_ID), eq("XP_100"), eq(NOW));
    }

    @Test
    void evaluate_nothingReached_insertsNothing() {
        givenStats(0, 0);

        service.evaluate(USER_ID);

        verify(userAchievementRepository, never()).insertIfAbsent(any(), any(), any());
    }

    @Test
    void evaluate_usesConfiguredPassScoreWhenCountingPassedQuizzes() {
        givenStats(0, 0);

        service.evaluate(USER_ID);

        verify(quizAttemptRepository).countDistinctPassedQuizzes(eq(USER_ID), eq(PASS_SCORE));
    }

    @Test
    void evaluate_allReached_insertsEachOfFiveCodesOnce() {
        givenStats(100, 7);
        when(quizAttemptRepository.countDistinctPassedQuizzes(USER_ID, PASS_SCORE)).thenReturn(10L);
        when(userVocabularyRepository.countByUser_IdAndStatus(USER_ID, VocabularyStatus.KNOWN)).thenReturn(50L);
        when(dictationResultRepository.countDistinctLessonsByUserId(USER_ID)).thenReturn(5L);

        service.evaluate(USER_ID);

        verify(userAchievementRepository, times(5)).insertIfAbsent(any(), any(), any());
        for (String code : new String[] { "XP_100", "STREAK_7", "QUIZ_PASS_10", "VOCAB_KNOWN_50", "DICTATION_5" }) {
            verify(userAchievementRepository, times(1)).insertIfAbsent(eq(USER_ID), eq(code), eq(NOW));
        }
    }

    @Test
    void evaluate_whenInsertReturnsZero_doesNotThrow() {
        givenStats(100, 0);
        when(userAchievementRepository.insertIfAbsent(any(), any(), any())).thenReturn(0);

        assertDoesNotThrow(() -> service.evaluate(USER_ID));
    }
}