package com.example.englishlearningplatform.repository;

import java.time.Instant;

public interface DictationLessonStats {
    Long getLessonId();

    Long getAttempts();

    Double getBestAccuracy();

    Instant getLastAttemptAt();
}