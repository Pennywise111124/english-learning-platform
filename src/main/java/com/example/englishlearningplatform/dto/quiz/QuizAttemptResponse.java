package com.example.englishlearningplatform.dto.quiz;

import com.example.englishlearningplatform.entity.QuizAttempt;

// GET /api/quizzes/{id}/attempts — dùng trong PageResponse<QuizAttemptResponse>
public class QuizAttemptResponse {

    private Long id;
    private int score;
    private int correctAnswers;
    private int totalQuestions;
    private java.time.Instant completedAt;

    public static QuizAttemptResponse from(QuizAttempt attempt) {
        QuizAttemptResponse dto = new QuizAttemptResponse();
        dto.id = attempt.getId();
        dto.score = attempt.getScore();
        dto.correctAnswers = attempt.getCorrectAnswers();
        dto.totalQuestions = attempt.getTotalQuestions();
        dto.completedAt = attempt.getCompletedAt();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public int getScore() {
        return score;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public java.time.Instant getCompletedAt() {
        return completedAt;
    }
}