package com.example.englishlearningplatform.service;

import com.example.englishlearningplatform.dto.chat.*;

import reactor.core.publisher.Flux;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {

    ConversationDetailResponse createConversation(String username);

    Page<ConversationSummaryResponse> getConversations(String username, Pageable pageable);

    ConversationDetailResponse getConversation(String username, Long conversationId);

    List<MessageResponse> getMessages(String username, Long conversationId);

    MessageResponse sendMessage(String username, Long conversationId, SendMessageRequest request);

    Flux<ChatStreamEvent> sendMessageStream(String username, Long conversationId, SendMessageRequest request);

    boolean isOwner(String username, Long conversationId);
}