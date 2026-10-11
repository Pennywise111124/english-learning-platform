package com.example.englishlearningplatform.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.englishlearningplatform.config.QuizProperties;
import com.example.englishlearningplatform.dto.achievement.AchievementResponse;
import com.example.englishlearningplatform.entity.AchievementCode;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.entity.UserAchievement;
import com.example.englishlearningplatform.entity.UserStats;
import com.example.englishlearningplatform.entity.VocabularyStatus;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.DictationResultRepository;
import com.example.englishlearningplatform.repository.QuizAttemptRepository;
import com.example.englishlearningplatform.repository.UserAchievementRepository;
import com.example.englishlearningplatform.repository.UserRepository;
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
    @Mock
    UserRepository userRepository;

    private AchievementService service;

    private static final String USERNAME = "alice";

    @BeforeEach
    void setUp() {
        service = new AchievementService(userStatsRepository, quizAttemptRepository,
                userVocabularyRepository, dictationResultRepository, userAchievementRepository, userRepository,
                new QuizProperties(PASS_SCORE), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void givenStats(int totalXp, int longestStreak) {
        UserStats stats = new UserStats();
        stats.setTotalXp(totalXp);
        stats.setLongestStreak(longestStreak);
        when(userStatsRepository.findById(USER_ID)).thenReturn(Optional.of(stats));
    }

    private void givenUser() {
        User u = new User();
        u.setId(USER_ID);
        u.setUsername(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
    }

    private UserAchievement row(AchievementCode code, Instant awardedAt, Instant dismissedAt) {
        UserAchievement ua = mock(UserAchievement.class);
        lenient().when(ua.getCode()).thenReturn(code);
        lenient().when(ua.getAwardedAt()).thenReturn(awardedAt);
        lenient().when(ua.getDismissedAt()).thenReturn(dismissedAt);
        return ua;
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

    @Test
    void getMyAchievements_returnsAllCodesInEnumOrder_withUnlockedAndDismissedState() {
        givenUser();
        Instant t = NOW.minusSeconds(3600);

        UserAchievement xpRow = row(AchievementCode.XP_100, t, NOW);
        UserAchievement streakRow = row(AchievementCode.STREAK_7, t, null);
        when(userAchievementRepository.findByUserId(USER_ID)).thenReturn(List.of(xpRow, streakRow));

        List<AchievementResponse> result = service.getMyAchievements(USERNAME);

        verify(userRepository).findByUsername(USERNAME);
        verify(userAchievementRepository).findByUserId(USER_ID);

        assertEquals(5, result.size());
        assertEquals(
                Arrays.stream(AchievementCode.values()).map(Enum::name).toList(),
                result.stream().map(AchievementResponse::code).toList());

        AchievementResponse xp = result.stream().filter(r -> r.code().equals("XP_100")).findFirst().orElseThrow();
        assertTrue(xp.unlocked());
        assertEquals(t, xp.awardedAt());
        assertTrue(xp.dismissed());

        AchievementResponse streak = result.stream().filter(r -> r.code().equals("STREAK_7")).findFirst().orElseThrow();
        assertTrue(streak.unlocked());
        assertFalse(streak.dismissed());

        AchievementResponse locked = result.stream().filter(r -> r.code().equals("QUIZ_PASS_10")).findFirst()
                .orElseThrow();
        assertFalse(locked.unlocked());
        assertNull(locked.awardedAt());
        assertFalse(locked.dismissed());
    }

    @Test
    void dismiss_unknownCode_throwsResourceNotFoundAndDoesNotTouchRepository() {
        assertThrows(ResourceNotFoundException.class, () -> service.dismiss(USERNAME, "NOT_A_CODE"));

        verify(userAchievementRepository, never()).dismiss(any(), any(), any());
    }

    @Test
    void dismiss_notUnlocked_throwsResourceNotFound() {
        givenUser();
        when(userAchievementRepository.dismiss(USER_ID, AchievementCode.XP_100, NOW)).thenReturn(0);
        when(userAchievementRepository.findByUserId(USER_ID)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> service.dismiss(USERNAME, "XP_100"));
    }

    @Test
    void dismiss_firstTime_succeedsAndUsesUserIdFromUsername() {
        givenUser();
        when(userAchievementRepository.dismiss(USER_ID, AchievementCode.XP_100, NOW)).thenReturn(1);

        assertDoesNotThrow(() -> service.dismiss(USERNAME, "XP_100"));

        verify(userAchievementRepository).dismiss(eq(USER_ID), eq(AchievementCode.XP_100), eq(NOW));
    }

    @Test
    void dismiss_alreadyDismissed_isIdempotent() {
        givenUser();
        UserAchievement dismissedRow = row(AchievementCode.XP_100, NOW.minusSeconds(60), NOW.minusSeconds(30));
        when(userAchievementRepository.dismiss(USER_ID, AchievementCode.XP_100, NOW)).thenReturn(0);
        when(userAchievementRepository.findByUserId(USER_ID)).thenReturn(List.of(dismissedRow));

        assertDoesNotThrow(() -> service.dismiss(USERNAME, "XP_100"));
    }
}