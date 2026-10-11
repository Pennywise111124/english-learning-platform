package com.example.englishlearningplatform.dto.auth;

import jakarta.validation.constraints.NotNull;

public class UpdateLeaderboardVisibilityRequest {

    @NotNull(message = "visible must not be null")
    private Boolean visible;

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}