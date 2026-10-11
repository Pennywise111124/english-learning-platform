package com.example.englishlearningplatform.dto.stats;

public record UserStatsResponse(long totalXp, int level, long xpToNextLevel,
        int currentStreak, int longestStreak, boolean activeToday, boolean freezeAvailable) {
}