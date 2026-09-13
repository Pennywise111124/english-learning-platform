package com.example.englishlearningplatform.dto.quiz;

import com.example.englishlearningplatform.entity.Quiz;

import java.util.List;

// GET /api/quizzes/{id} — chi tiết đề thi cho User, dùng QuizQuestionPublicResponse
// nên không thể vô tình lộ correctAnswer
public class QuizDetailResponse {

    private Long id;
    private String title;
    private List<QuizQuestionPublicResponse> questions;

    public static QuizDetailResponse of(Quiz quiz, List<QuizQuestionPublicResponse> questions) {
        QuizDetailResponse dto = new QuizDetailResponse();
        dto.id = quiz.getId();
        dto.title = quiz.getTitle();
        dto.questions = questions;
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<QuizQuestionPublicResponse> getQuestions() {
        return questions;
    }
}