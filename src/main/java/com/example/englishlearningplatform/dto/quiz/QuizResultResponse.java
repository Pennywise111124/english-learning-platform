package com.example.englishlearningplatform.dto.quiz;

import java.time.Instant;

public class QuizResultResponse {

    private int score;
    private int correctAnswers;
    private int totalQuestions;
    private Instant completedAt;

    public QuizResultResponse(int score, int correctAnswers, int totalQuestions, Instant completedAt) {
        this.score = score;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.completedAt = completedAt;
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

    public Instant getCompletedAt() {
        return completedAt;
    }
}