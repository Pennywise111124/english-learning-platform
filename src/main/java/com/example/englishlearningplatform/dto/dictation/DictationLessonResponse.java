package com.example.englishlearningplatform.dto.dictation;

import com.example.englishlearningplatform.entity.DictationLesson;
import com.example.englishlearningplatform.entity.Level;

public record DictationLessonResponse(Long id, Long topicId, String title, String mediaUrl, Level level) {
    public static DictationLessonResponse from(DictationLesson l) {
        return new DictationLessonResponse(l.getId(), l.getTopic().getId(), l.getTitle(), l.getMediaUrl(),
                l.getLevel());
    }
}
