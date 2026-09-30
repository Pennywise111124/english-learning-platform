package com.example.englishlearningplatform.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Current password must not be blank!")
    private String currentPassword;

    @NotBlank(message = "New password must not be blank!")
    @Size(min = 6, message = "New password must be at least 6 characters!")
    private String newPassword;
}