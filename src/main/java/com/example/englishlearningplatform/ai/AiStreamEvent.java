package com.example.englishlearningplatform.ai;

public sealed interface AiStreamEvent {

    record ReplyChunk(String text) implements AiStreamEvent {
    }

    record Complete(AiChatResult result) implements AiStreamEvent {
    }
}