package com.example.englishlearningplatform.dto.dictation;

import com.example.englishlearningplatform.entity.DictationLesson;
import com.example.englishlearningplatform.entity.Level;

public record AdminDictationLessonResponse(Long id, Long topicId, String title, String mediaUrl,
        String transcript, Level level) {
    public static AdminDictationLessonResponse from(DictationLesson l) {
        return new AdminDictationLessonResponse(l.getId(), l.getTopic().getId(), l.getTitle(),
                l.getMediaUrl(), l.getTranscript(), l.getLevel());
    }
}
