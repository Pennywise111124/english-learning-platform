package com.example.englishlearningplatform.ai;

import java.util.List;

import reactor.core.publisher.Flux;

public interface AiClient {

    AiChatResult chat(List<AiChatMessage> history);

    Flux<AiStreamEvent> chatStream(List<AiChatMessage> history);
}