package com.example.englishlearningplatform.dto.vocabulary;

import jakarta.validation.constraints.NotNull;

public class ReviewRequest {

    @NotNull(message = "remembered is required")
    private Boolean remembered;

    public Boolean getRemembered() {
        return remembered;
    }

    public void setRemembered(Boolean remembered) {
        this.remembered = remembered;
    }
}
