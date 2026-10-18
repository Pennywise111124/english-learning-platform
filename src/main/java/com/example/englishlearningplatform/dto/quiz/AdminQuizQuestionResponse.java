package com.example.englishlearningplatform.dto.quiz;

import com.example.englishlearningplatform.entity.QuizQuestion;

import java.util.List;

public class AdminQuizQuestionResponse {

    private Long id;
    private String question;
    private List<String> options;
    private String correctAnswer;

    public static AdminQuizQuestionResponse from(QuizQuestion question) {
        AdminQuizQuestionResponse dto = new AdminQuizQuestionResponse();
        dto.id = question.getId();
        dto.question = question.getQuestion();
        dto.options = question.getOptions();
        dto.correctAnswer = question.getCorrectAnswer();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }
}