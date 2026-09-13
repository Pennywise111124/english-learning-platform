package com.example.englishlearningplatform.dto.chat;

import java.time.Instant;

public record ConversationSummaryResponse(Long id, String title, Instant updatedAt) {
}