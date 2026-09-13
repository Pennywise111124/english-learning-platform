package com.example.englishlearningplatform.dto.chat;

import com.example.englishlearningplatform.entity.Sender;

import java.time.Instant;

public record MessageResponse(Long id, Sender sender, String content,
                String correction, String explanation, Instant createdAt) {
}