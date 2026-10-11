package com.example.englishlearningplatform.service;

import java.util.EnumSet;
import java.util.Set;

import com.example.englishlearningplatform.entity.AchievementCode;

public final class AchievementRules {

    private static final long XP_THRESHOLD = 100;
    private static final long STREAK_THRESHOLD = 7;
    private static final long QUIZ_PASS_THRESHOLD = 10;
    private static final long VOCAB_KNOWN_THRESHOLD = 50;
    private static final long DICTATION_THRESHOLD = 5;

    private AchievementRules() {
    }

    public record Metrics(long totalXp, int longestStreak, long passedQuizzes,
            long knownWords, long distinctDictations) {
    }

    public static Set<AchievementCode> earned(Metrics m) {
        Set<AchievementCode> result = EnumSet.noneOf(AchievementCode.class);
        if (m.totalXp() >= XP_THRESHOLD) {
            result.add(AchievementCode.XP_100);
        }
        if (m.longestStreak() >= STREAK_THRESHOLD) {
            result.add(AchievementCode.STREAK_7);
        }
        if (m.passedQuizzes() >= QUIZ_PASS_THRESHOLD) {
            result.add(AchievementCode.QUIZ_PASS_10);
        }
        if (m.knownWords() >= VOCAB_KNOWN_THRESHOLD) {
            result.add(AchievementCode.VOCAB_KNOWN_50);
        }
        if (m.distinctDictations() >= DICTATION_THRESHOLD) {
            result.add(AchievementCode.DICTATION_5);
        }
        return result;
    }
}