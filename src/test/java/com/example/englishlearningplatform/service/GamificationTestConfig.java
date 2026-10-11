package com.example.englishlearningplatform.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.example.englishlearningplatform.config.GamificationProperties;
import com.example.englishlearningplatform.config.QuizProperties;

@TestConfiguration
@EnableConfigurationProperties({ GamificationProperties.class, QuizProperties.class })
class GamificationTestConfig {

    static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");

    @Bean
    @Primary
    Clock testClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}