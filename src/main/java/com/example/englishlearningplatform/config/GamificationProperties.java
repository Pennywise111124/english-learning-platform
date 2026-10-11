package com.example.englishlearningplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.gamification")
public record GamificationProperties(
                @Positive int xpQuizParticipation,
                @Positive int xpQuizPass,
                @Positive int xpVocabReview,
                @Positive int xpDictationMax,
                @Positive int quizPassScore,
                @Positive int quizParticipationDailyLimit) {
}