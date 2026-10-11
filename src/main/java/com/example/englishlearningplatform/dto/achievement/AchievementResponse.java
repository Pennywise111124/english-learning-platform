package com.example.englishlearningplatform.dto.achievement;

import java.time.Instant;

public record AchievementResponse(String code, String title, String description,
        boolean unlocked, Instant awardedAt, boolean dismissed) {
}