package com.example.englishlearningplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @Positive int loginPerMinute,
        @Positive int registerPerHour,
        @Positive int chatPerMinute,
        boolean trustedProxy) {
}