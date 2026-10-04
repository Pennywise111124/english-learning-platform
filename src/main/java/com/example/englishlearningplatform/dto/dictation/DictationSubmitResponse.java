package com.example.englishlearningplatform.dto.dictation;

import java.time.Instant;
import java.util.List;

public record DictationSubmitResponse(Long resultId, Long lessonId, double accuracy, String transcript,
        List<DictationWordResult> words, Instant createdAt) {
}
