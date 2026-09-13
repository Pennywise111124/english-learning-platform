package com.example.englishlearningplatform.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.example.englishlearningplatform.ai.AiProviderException;
import com.example.englishlearningplatform.dto.chat.ChatStreamEvent;
import com.example.englishlearningplatform.dto.chat.SendMessageRequest;
import com.example.englishlearningplatform.exception.ErrorResponse;
import com.example.englishlearningplatform.service.ChatService;

@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    // Client gửi tới: /app/chat.sendMessage/{conversationId}
    // Client nhận reply tại: /topic/conversations/{conversationId}
    @MessageMapping("/chat.sendMessage/{conversationId}")
    public void sendMessage(@DestinationVariable Long conversationId,
            @Payload SendMessageRequest request,
            Principal principal) {

        String username = principal.getName();

        chatService.sendMessageStream(username, conversationId, request)
                .subscribe(
                        event -> handleStreamEvent(conversationId, event),
                        ex -> handleStreamError(conversationId, username, ex));
    }

    private void handleStreamEvent(Long conversationId, ChatStreamEvent event) {
        switch (event) {
            case ChatStreamEvent.TypingChunk chunk ->
                messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/typing", chunk.text());
            case ChatStreamEvent.Done done ->
                messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, done.message());
        }
    }

    private void handleStreamError(Long conversationId, String username, Throwable ex) {
        log.error("Lỗi trong lúc stream chat cho user [{}], conversation [{}]: ", username, conversationId, ex);
        String message = ex instanceof AiProviderException
                ? ex.getMessage()
                : "Có lỗi hệ thống xảy ra. Vui lòng thử lại sau";

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/errors",
                new ErrorResponse(message, 500));
    }
}