package com.example.englishlearningplatform.dto.chat;

public sealed interface ChatStreamEvent {

    record TypingChunk(String text) implements ChatStreamEvent {
    }

    record Done(MessageResponse message) implements ChatStreamEvent {
    }
}