package com.example.englishlearningplatform.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.englishlearningplatform.dto.chat.SendMessageRequest;
import com.example.englishlearningplatform.exception.ErrorResponse;
import com.example.englishlearningplatform.exception.RateLimitExceededException;
import com.example.englishlearningplatform.ratelimit.RateLimitService;
import com.example.englishlearningplatform.service.ChatService;

import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    private static final String USER = "alice";
    private static final Long CONVERSATION_ID = 50L;

    @Mock
    ChatService chatService;
    @Mock
    SimpMessagingTemplate messagingTemplate;
    @Mock
    RateLimitService rateLimitService;
    @InjectMocks
    ChatWebSocketController controller;

    private final Principal principal = () -> USER;
    private final SendMessageRequest request = new SendMessageRequest("hi");

    @Test
    void rateLimited_sendsErrorToUserQueueAndNeverCallsChatService() {
        doThrow(new RateLimitExceededException(30)).when(rateLimitService).checkChat(USER);

        lenient().when(chatService.sendMessageStream(USER, CONVERSATION_ID, request)).thenReturn(Flux.empty());

        controller.sendMessage(CONVERSATION_ID, request, principal);

        verifyNoInteractions(chatService);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(eq(USER), eq("/queue/errors"), payload.capture());
        ErrorResponse error = assertInstanceOf(ErrorResponse.class, payload.getValue());
        assertEquals(429, error.getStatus());
        assertEquals("Too many requests. Please try again later", error.getMessage());
    }

    @Test
    void allowed_checksLimitBeforeCallingChatService() {
        when(chatService.sendMessageStream(USER, CONVERSATION_ID, request)).thenReturn(Flux.empty());

        controller.sendMessage(CONVERSATION_ID, request, principal);

        InOrder order = inOrder(rateLimitService, chatService);
        order.verify(rateLimitService).checkChat(USER);
        order.verify(chatService).sendMessageStream(USER, CONVERSATION_ID, request);
    }
}