package com.example.englishlearningplatform.dto.dictation;

import java.time.Instant;

import com.example.englishlearningplatform.entity.DictationLesson;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.repository.DictationLessonStats;

public record DictationCatalogItem(
        Long id, Long topicId, String topicTitle, String title, String mediaUrl, Level level,
        long attempts, Double bestAccuracy, Instant lastAttemptAt) {

    public static DictationCatalogItem of(DictationLesson lesson, DictationLessonStats stats) {
        return new DictationCatalogItem(
                lesson.getId(),
                lesson.getTopic().getId(),
                lesson.getTopic().getTitle(),
                lesson.getTitle(),
                lesson.getMediaUrl(),
                lesson.getLevel(),
                stats == null ? 0L : stats.getAttempts(),
                stats == null ? null : stats.getBestAccuracy(),
                stats == null ? null : stats.getLastAttemptAt());
    }
}