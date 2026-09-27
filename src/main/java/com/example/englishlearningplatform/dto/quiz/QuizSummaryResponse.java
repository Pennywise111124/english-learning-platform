package com.example.englishlearningplatform.dto.quiz;

import com.example.englishlearningplatform.entity.Quiz;

// Dùng cho GET /api/topics/{id}/quizzes — chỉ id + title, KHÔNG kèm câu hỏi
// (đúng shape mẫu ở FE_Handoff_Brief.md: [{ "id": 1, "title": "..." }])
public class QuizSummaryResponse {

    private Long id;
    private String title;
    private long questionCount;

    public static QuizSummaryResponse from(Quiz quiz, long questionCount) {
        QuizSummaryResponse dto = new QuizSummaryResponse();
        dto.id = quiz.getId();
        dto.title = quiz.getTitle();
        dto.questionCount = questionCount;
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
}