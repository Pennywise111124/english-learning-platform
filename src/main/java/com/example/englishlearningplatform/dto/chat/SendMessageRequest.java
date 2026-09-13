package com.example.englishlearningplatform.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(@NotBlank String content) {
}