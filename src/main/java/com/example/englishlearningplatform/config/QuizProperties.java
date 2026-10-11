package com.example.englishlearningplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "app.quiz")
public record QuizProperties(@Min(1) @Max(100) int passScore) {
}