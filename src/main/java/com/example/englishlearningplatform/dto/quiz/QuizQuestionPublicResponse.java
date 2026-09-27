package com.example.englishlearningplatform.dto.quiz;

import com.example.englishlearningplatform.entity.QuizQuestion;

public class QuizQuestionPublicResponse {

    private Long id;
    private String question;
    private java.util.List<String> options;

    public static QuizQuestionPublicResponse from(QuizQuestion question) {
        QuizQuestionPublicResponse dto = new QuizQuestionPublicResponse();
        dto.id = question.getId();
        dto.question = question.getQuestion();
        dto.options = question.getOptions();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public java.util.List<String> getOptions() {
        return options;
    }
}