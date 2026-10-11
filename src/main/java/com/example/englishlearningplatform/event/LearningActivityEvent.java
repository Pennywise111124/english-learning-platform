package com.example.englishlearningplatform.event;

import java.time.Instant;

public record LearningActivityEvent(Long userId, Kind kind, Long refId, double metric, Instant occurredAt) {

    public enum Kind {
        QUIZ_SUBMITTED, VOCAB_REVIEWED, DICTATION_SUBMITTED
    }

    public static LearningActivityEvent quizSubmitted(Long userId, Long quizId, int score, Instant at) {
        return new LearningActivityEvent(userId, Kind.QUIZ_SUBMITTED, quizId, score, at);
    }

    public static LearningActivityEvent vocabReviewed(Long userId, Long userVocabularyId, boolean remembered,
            Instant at) {
        return new LearningActivityEvent(userId, Kind.VOCAB_REVIEWED, userVocabularyId, remembered ? 1 : 0, at);
    }

    public static LearningActivityEvent dictationSubmitted(Long userId, Long lessonId, double accuracy, Instant at) {
        return new LearningActivityEvent(userId, Kind.DICTATION_SUBMITTED, lessonId, accuracy, at);
    }
}