package com.example.englishlearningplatform.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.englishlearningplatform.entity.AchievementCode;
import com.example.englishlearningplatform.service.AchievementRules.Metrics;

class AchievementRulesTest {

    private static Metrics m(long xp, int streak, long quiz, long vocab, long dict) {
        return new Metrics(xp, streak, quiz, vocab, dict);
    }

    @Test
    void allZero_earnsNothing() {
        assertThat(AchievementRules.earned(m(0, 0, 0, 0, 0))).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "99, false", "100, true" })
    void xp100_boundary(long xp, boolean expected) {
        assertThat(AchievementRules.earned(m(xp, 0, 0, 0, 0)).contains(AchievementCode.XP_100))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "6, false", "7, true" })
    void streak7_boundary(int streak, boolean expected) {
        assertThat(AchievementRules.earned(m(0, streak, 0, 0, 0)).contains(AchievementCode.STREAK_7))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "9, false", "10, true" })
    void quizPass10_boundary(long quiz, boolean expected) {
        assertThat(AchievementRules.earned(m(0, 0, quiz, 0, 0)).contains(AchievementCode.QUIZ_PASS_10))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "49, false", "50, true" })
    void vocabKnown50_boundary(long vocab, boolean expected) {
        assertThat(AchievementRules.earned(m(0, 0, 0, vocab, 0)).contains(AchievementCode.VOCAB_KNOWN_50))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "4, false", "5, true" })
    void dictation5_boundary(long dict, boolean expected) {
        assertThat(AchievementRules.earned(m(0, 0, 0, 0, dict)).contains(AchievementCode.DICTATION_5))
                .isEqualTo(expected);
    }

    @Test
    void onlyXp_earnsOnlyXp100() {
        assertThat(AchievementRules.earned(m(1000, 0, 0, 0, 0)))
                .containsExactly(AchievementCode.XP_100);
    }

    @Test
    void onlyVocab_earnsOnlyVocabKnown50() {
        assertThat(AchievementRules.earned(m(0, 0, 0, 500, 0)))
                .containsExactly(AchievementCode.VOCAB_KNOWN_50);
    }

    @Test
    void allThresholdsMet_earnsAllFive() {
        assertThat(AchievementRules.earned(m(100, 7, 10, 50, 5)))
                .containsExactlyInAnyOrderElementsOf(EnumSet.allOf(AchievementCode.class));
    }

    @Test
    void hugeXp_doesNotOverflow() {
        assertThat(AchievementRules.earned(m(Long.MAX_VALUE, 0, 0, 0, 0)))
                .contains(AchievementCode.XP_100);
    }
}