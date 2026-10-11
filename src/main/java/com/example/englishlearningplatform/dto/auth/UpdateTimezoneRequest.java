package com.example.englishlearningplatform.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateTimezoneRequest {

    @NotBlank(message = "Timezone must not be blank")
    @Size(max = 64, message = "Timezone must be at most 64 characters")
    private String timezone;

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}