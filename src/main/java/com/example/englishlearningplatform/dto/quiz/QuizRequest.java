package com.example.englishlearningplatform.dto.quiz;

import jakarta.validation.constraints.NotBlank;

public class QuizRequest {

    @NotBlank
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}