package com.example.englishlearningplatform.dto.chat;

import java.time.Instant;

public record ConversationDetailResponse(Long id, String title, Instant createdAt, Instant updatedAt) {
}