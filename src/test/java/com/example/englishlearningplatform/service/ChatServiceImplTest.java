package com.example.englishlearningplatform.service;

import com.example.englishlearningplatform.ai.AiChatMessage;
import com.example.englishlearningplatform.ai.AiChatResult;
import com.example.englishlearningplatform.ai.AiClient;
import com.example.englishlearningplatform.ai.AiStreamEvent;
import com.example.englishlearningplatform.config.AiProperties;
import com.example.englishlearningplatform.dto.chat.ChatStreamEvent;
import com.example.englishlearningplatform.dto.chat.ConversationDetailResponse;
import com.example.englishlearningplatform.dto.chat.ConversationSummaryResponse;
import com.example.englishlearningplatform.dto.chat.MessageResponse;
import com.example.englishlearningplatform.dto.chat.SendMessageRequest;
import com.example.englishlearningplatform.entity.Conversation;
import com.example.englishlearningplatform.entity.Message;
import com.example.englishlearningplatform.entity.Sender;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.ConversationRepository;
import com.example.englishlearningplatform.repository.MessageRepository;
import com.example.englishlearningplatform.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AiClient aiClient;
    @Mock
    private AiProperties aiProperties;

    @InjectMocks
    private ChatServiceImpl chatService;

    private static final String USERNAME = "testuser";
    private static final Long CONVERSATION_ID = 50L;

    private User testUser;
    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(USERNAME);

        testConversation = new Conversation(testUser);
    }

    private void setupSendMessageCommonStubs() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(messageRepository.findByConversationOrderByCreatedAtAsc(testConversation))
                .thenReturn(Collections.emptyList());
        when(aiProperties.getContextMessageLimit()).thenReturn(10);
    }

    // ------------------------------------------------------------------
    // createConversation()
    // ------------------------------------------------------------------

    @Test
    void createConversation_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chatService.createConversation(USERNAME));

        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createConversation_happyPath_shouldSaveAndReturnMappedResponse() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        ConversationDetailResponse response = chatService.createConversation(USERNAME);

        assertNotNull(response);
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertEquals(testUser, captor.getValue().getUser());
    }

    // ------------------------------------------------------------------
    // getConversations()
    // ------------------------------------------------------------------

    @Test
    void getConversations_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(ResourceNotFoundException.class, () -> chatService.getConversations(USERNAME, pageable));
    }

    @Test
    void getConversations_happyPath_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Conversation> conversationPage = new PageImpl<>(List.of(testConversation), pageable, 1);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByUser(testUser, pageable)).thenReturn(conversationPage);

        Page<ConversationSummaryResponse> result = chatService.getConversations(USERNAME, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testConversation.getTitle(), result.getContent().get(0).title());
    }

    // ------------------------------------------------------------------
    // getConversation()
    // ------------------------------------------------------------------

    @Test
    void getConversation_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chatService.getConversation(USERNAME, CONVERSATION_ID));
    }

    @Test
    void getConversation_whenNotFoundOrNotOwned_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chatService.getConversation(USERNAME, CONVERSATION_ID));
    }

    @Test
    void getConversation_happyPath_shouldReturnMappedDetailResponse() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.of(testConversation));

        ConversationDetailResponse result = chatService.getConversation(USERNAME, CONVERSATION_ID);

        assertNotNull(result);
        assertEquals(testConversation.getTitle(), result.title());
    }

    // ------------------------------------------------------------------
    // getMessages()
    // ------------------------------------------------------------------

    @Test
    void getMessages_whenNotFoundOrNotOwned_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chatService.getMessages(USERNAME, CONVERSATION_ID));
    }

    @Test
    void getMessages_happyPath_shouldReturnOrderedMappedList() {
        Message userMsg = Message.ofUser(testConversation, "User message");
        Message aiMsg = Message.ofAi(testConversation, "AI response", null, null);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationOrderByCreatedAtAsc(testConversation))
                .thenReturn(List.of(userMsg, aiMsg));

        List<MessageResponse> result = chatService.getMessages(USERNAME, CONVERSATION_ID);

        assertEquals(2, result.size());
        assertEquals("User message", result.get(0).content());
        assertEquals(Sender.USER, result.get(0).sender());
        assertEquals("AI response", result.get(1).content());
        assertEquals(Sender.AI, result.get(1).sender());
    }

    // ------------------------------------------------------------------
    // sendMessage() — phần trọng tâm nhất của service này
    // ------------------------------------------------------------------

    @Test
    void sendMessage_whenConversationNotFound_shouldThrowAndNotSaveAnything() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.empty());
        SendMessageRequest request = new SendMessageRequest("Hello");

        assertThrows(ResourceNotFoundException.class,
                () -> chatService.sendMessage(USERNAME, CONVERSATION_ID, request));

        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_happyPath_shouldSaveUserMessageBeforeCallingAiThenSaveAiMessage() {
        SendMessageRequest request = new SendMessageRequest("Hello AI");

        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.of(testConversation));
        setupSendMessageCommonStubs();

        AiChatResult chatResult = new AiChatResult("AI reply here", null, null);
        when(aiClient.chat(anyList())).thenReturn(chatResult);

        MessageResponse response = chatService.sendMessage(USERNAME, CONVERSATION_ID, request);

        assertNotNull(response);
        assertEquals(Sender.AI, response.sender());
        assertEquals("AI reply here", response.content());

        InOrder inOrder = inOrder(messageRepository, aiClient);
        inOrder.verify(messageRepository).save(argThat(m -> m.getSender() == Sender.USER));
        inOrder.verify(aiClient).chat(anyList());
        inOrder.verify(messageRepository).save(argThat(m -> m.getSender() == Sender.AI));
    }

    @Test
    void sendMessage_whenAiThrows_shouldKeepUserMessageSavedButNotSaveAiMessage() {
        SendMessageRequest request = new SendMessageRequest("Hello AI");

        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.of(testConversation));
        setupSendMessageCommonStubs();
        when(aiClient.chat(anyList())).thenThrow(new RuntimeException("xKiro sập"));

        assertThrows(RuntimeException.class, () -> chatService.sendMessage(USERNAME, CONVERSATION_ID, request));

        verify(messageRepository, times(1)).save(argThat(m -> m.getSender() == Sender.USER));
        verify(messageRepository, never()).save(argThat(m -> m.getSender() == Sender.AI));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void sendMessage_whenConversationHasNoTitle_shouldSetTitleFromShortContent() {
        SendMessageRequest request = new SendMessageRequest("Short content");

        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.of(testConversation));
        setupSendMessageCommonStubs();
        when(aiClient.chat(anyList())).thenReturn(new AiChatResult("AI response", null, null));

        chatService.sendMessage(USERNAME, CONVERSATION_ID, request);

        assertEquals("Short content", testConversation.getTitle());
    }

    @Test
    void sendMessage_whenContentExceedsTitleMaxLength_shouldTruncateTitleWithEllipsis() {
        String longContent = "a".repeat(60);
        SendMessageRequest request = new SendMessageRequest(longContent);

        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.of(testConversation));
        setupSendMessageCommonStubs();
        when(aiClient.chat(anyList())).thenReturn(new AiChatResult("AI response", null, null));

        chatService.sendMessage(USERNAME, CONVERSATION_ID, request);

        assertNotNull(testConversation.getTitle());
        assertEquals(53, testConversation.getTitle().length()); // 50 ký tự + "..."
        assertTrue(testConversation.getTitle().endsWith("..."));
    }

    @Test
    void sendMessage_whenConversationAlreadyHasTitle_shouldNotOverwriteTitle() {
        testConversation.setTitle("Existing Title");
        SendMessageRequest request = new SendMessageRequest("New message content");

        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.of(testConversation));
        setupSendMessageCommonStubs();
        when(aiClient.chat(anyList())).thenReturn(new AiChatResult("AI response", null, null));

        chatService.sendMessage(USERNAME, CONVERSATION_ID, request);

        assertEquals("Existing Title", testConversation.getTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMessage_shouldLimitAiContextToConfiguredMessageLimit() {
        List<Message> history = List.of(
                Message.ofUser(testConversation, "msg 1"),
                Message.ofAi(testConversation, "msg 2", null, null),
                Message.ofUser(testConversation, "msg 3"),
                Message.ofAi(testConversation, "msg 4", null, null),
                Message.ofUser(testConversation, "msg 5 - tin nhan moi nhat"));

        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationOrderByCreatedAtAsc(testConversation))
                .thenReturn(history);
        when(aiProperties.getContextMessageLimit()).thenReturn(2);
        when(aiClient.chat(anyList())).thenReturn(new AiChatResult("AI reply", null, null));

        SendMessageRequest request = new SendMessageRequest("ignored, history da fix san o tren");

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        chatService.sendMessage(USERNAME, CONVERSATION_ID, request);

        ArgumentCaptor<List<AiChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiClient).chat(captor.capture());

        List<AiChatMessage> sentContext = captor.getValue();
        assertEquals(2, sentContext.size());
        assertEquals("msg 4", sentContext.get(0).content());
        assertEquals("msg 5 - tin nhan moi nhat", sentContext.get(1).content());
    }

    // ------------------------------------------------------------------
    // sendMessageStream()
    // ------------------------------------------------------------------

    @Test
    void sendMessageStream_whenConversationNotFound_shouldThrowImmediately() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.empty());
        SendMessageRequest request = new SendMessageRequest("Hello");

        assertThrows(ResourceNotFoundException.class,
                () -> chatService.sendMessageStream(USERNAME, CONVERSATION_ID, request));
    }

    @Test
    void sendMessageStream_happyPath_shouldEmitTypingChunksThenDoneAndPersistAiMessage() {
        SendMessageRequest request = new SendMessageRequest("Hello AI");

        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.of(testConversation));
        setupSendMessageCommonStubs();

        AiChatResult finalResult = new AiChatResult("Hello World", null, null);
        when(aiClient.chatStream(anyList())).thenReturn(Flux.just(
                new AiStreamEvent.ReplyChunk("Hello "),
                new AiStreamEvent.ReplyChunk("World"),
                new AiStreamEvent.Complete(finalResult)));

        List<ChatStreamEvent> events = chatService
                .sendMessageStream(USERNAME, CONVERSATION_ID, request)
                .collectList()
                .block();

        assertNotNull(events);
        assertEquals(3, events.size());

        assertTrue(events.get(0) instanceof ChatStreamEvent.TypingChunk);
        assertEquals("Hello ", ((ChatStreamEvent.TypingChunk) events.get(0)).text());

        assertTrue(events.get(1) instanceof ChatStreamEvent.TypingChunk);
        assertEquals("World", ((ChatStreamEvent.TypingChunk) events.get(1)).text());

        assertTrue(events.get(2) instanceof ChatStreamEvent.Done);
        ChatStreamEvent.Done doneEvent = (ChatStreamEvent.Done) events.get(2);
        assertEquals(Sender.AI, doneEvent.message().sender());
        assertEquals("Hello World", doneEvent.message().content());

        verify(messageRepository).save(argThat(m -> m.getSender() == Sender.AI));
        verify(conversationRepository).save(testConversation);
    }

    // ------------------------------------------------------------------
    // isOwner()
    // ------------------------------------------------------------------

    @Test
    void isOwner_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chatService.isOwner(USERNAME, CONVERSATION_ID));
    }

    @Test
    void isOwner_whenOwned_shouldReturnTrue() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.of(testConversation));

        assertTrue(chatService.isOwner(USERNAME, CONVERSATION_ID));
    }

    @Test
    void isOwner_whenNotOwned_shouldReturnFalse() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByIdAndUser(CONVERSATION_ID, testUser))
                .thenReturn(Optional.empty());

        assertFalse(chatService.isOwner(USERNAME, CONVERSATION_ID));
    }
}