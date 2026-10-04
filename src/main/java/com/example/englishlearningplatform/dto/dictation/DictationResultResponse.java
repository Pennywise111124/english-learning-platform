package com.example.englishlearningplatform.dto.dictation;

import java.time.Instant;

import com.example.englishlearningplatform.entity.DictationResult;

public record DictationResultResponse(Long id, Long lessonId, String userInput, double accuracy, Instant createdAt) {
    public static DictationResultResponse from(DictationResult r) {
        return new DictationResultResponse(r.getId(), r.getLesson().getId(), r.getUserInput(),
                r.getAccuracy(), r.getCreatedAt());
    }
}
