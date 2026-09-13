package com.example.englishlearningplatform.dto.quiz;

import com.example.englishlearningplatform.entity.QuizQuestion;

public class QuizQuestionPublicResponse {

    private Long id;
    private String question;
    private java.util.List<String> options;

    // Cố ý KHÔNG có trường correctAnswer — đây chính là cách "ẩn hẳn, không chỉ
    // ẩn ở tầng serialize" mà FR-4.5 yêu cầu. Dù lập trình viên viết
    // @JsonIgnore trên entity cũng có nguy cơ quên, còn class này về mặt CẤU
    // TRÚC không có chỗ để nhét correctAnswer vào, không thể vô tình lộ ra.
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