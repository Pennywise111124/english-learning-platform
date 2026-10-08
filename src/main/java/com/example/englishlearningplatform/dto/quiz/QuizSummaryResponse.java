package com.example.englishlearningplatform.dto.quiz;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Quiz;

public class QuizSummaryResponse {

    private Long id;
    private String title;
    private long questionCount;
    private ContentStatus status;

    public static QuizSummaryResponse from(Quiz quiz, long questionCount) {
        QuizSummaryResponse dto = new QuizSummaryResponse();
        dto.id = quiz.getId();
        dto.title = quiz.getTitle();
        dto.questionCount = questionCount;
        dto.status = quiz.getStatus();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public long getQuestionCount() {
        return questionCount;
    }

    public ContentStatus getStatus() {
        return status;
    }
}