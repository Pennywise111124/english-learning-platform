package com.example.englishlearningplatform.service;

import com.example.englishlearningplatform.ai.AiChatMessage;
import com.example.englishlearningplatform.ai.AiChatResult;
import com.example.englishlearningplatform.ai.AiClient;
import com.example.englishlearningplatform.ai.AiStreamEvent;
import com.example.englishlearningplatform.config.AiProperties;
import com.example.englishlearningplatform.dto.chat.*;
import com.example.englishlearningplatform.entity.Conversation;
import com.example.englishlearningplatform.entity.Message;
import com.example.englishlearningplatform.entity.Sender;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.ConversationRepository;
import com.example.englishlearningplatform.repository.MessageRepository;
import com.example.englishlearningplatform.repository.UserRepository;

import reactor.core.publisher.Flux;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private static final int TITLE_MAX_LENGTH = 50;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiClient aiClient;
    private final AiProperties aiProperties;

    public ChatServiceImpl(ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            AiClient aiClient,
            AiProperties aiProperties) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
    }

    @Override
    public ConversationDetailResponse createConversation(String username) {
        User user = loadUser(username);
        Conversation conversation = new Conversation(user);
        conversationRepository.save(conversation);
        return toDetailResponse(conversation);
    }

    @Override
    public Page<ConversationSummaryResponse> getConversations(String username, Pageable pageable) {
        User user = loadUser(username);
        return conversationRepository.findByUser(user, pageable)
                .map(this::toSummaryResponse);
    }

    @Override
    public ConversationDetailResponse getConversation(String username, Long conversationId) {
        Conversation conversation = loadOwnedConversation(username, conversationId);
        return toDetailResponse(conversation);
    }

    @Override
    public List<MessageResponse> getMessages(String username, Long conversationId) {
        Conversation conversation = loadOwnedConversation(username, conversationId);
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Override
    public MessageResponse sendMessage(String username, Long conversationId, SendMessageRequest request) {
        Conversation conversation = loadOwnedConversation(username, conversationId);

        // FR-2.6: lưu Message của User NGAY, độc lập với việc gọi AI có thành công
        // hay không. messageRepository.save() tự mở/commit transaction riêng
        // (SimpleJpaRepository đã @Transactional sẵn theo từng lệnh gọi), nên
        // method này KHÔNG đánh @Transactional bao trùm toàn bộ.
        Message userMessage = Message.ofUser(conversation, request.content());
        messageRepository.save(userMessage);

        List<Message> history = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        List<AiChatMessage> context = toAiContext(history, aiProperties.getContextMessageLimit());

        // Nếu lỗi, AiClientImpl ném AiProviderException (unchecked) — bay thẳng
        // lên GlobalExceptionHandler (503). userMessage ở trên đã lưu, KHÔNG
        // rollback — đúng FR-2.6.
        AiChatResult result = aiClient.chat(context);

        Message aiMessage = Message.ofAi(conversation, result.reply(), result.correction(), result.explanation());
        messageRepository.save(aiMessage);

        if (conversation.getTitle() == null) {
            conversation.setTitle(buildTitle(request.content()));
        }

        conversation.touch();
        conversationRepository.save(conversation);

        return toMessageResponse(aiMessage);
    }

    @Override
    public Flux<ChatStreamEvent> sendMessageStream(String username, Long conversationId, SendMessageRequest request) {
        Conversation conversation = loadOwnedConversation(username, conversationId);

        Message userMessage = Message.ofUser(conversation, request.content());
        messageRepository.save(userMessage);

        List<Message> history = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        List<AiChatMessage> context = toAiContext(history, aiProperties.getContextMessageLimit());

        return aiClient.chatStream(context)
                .map(event -> switch (event) {
                    case AiStreamEvent.ReplyChunk chunk -> new ChatStreamEvent.TypingChunk(chunk.text());
                    case AiStreamEvent.Complete complete -> {
                        AiChatResult result = complete.result();

                        Message aiMessage = Message.ofAi(conversation, result.reply(), result.correction(),
                                result.explanation());
                        messageRepository.save(aiMessage);

                        if (conversation.getTitle() == null) {
                            conversation.setTitle(buildTitle(request.content()));
                        }
                        conversation.touch();
                        conversationRepository.save(conversation);

                        yield new ChatStreamEvent.Done(toMessageResponse(aiMessage));
                    }
                });
    }

    @Override
    public boolean isOwner(String username, Long conversationId) {
        User user = loadUser(username);
        return conversationRepository.findByIdAndUser(conversationId, user).isPresent();
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại: " + username));
    }

    private Conversation loadOwnedConversation(String username, Long conversationId) {
        User user = loadUser(username);
        return conversationRepository.findByIdAndUser(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation không tồn tại: " + conversationId));
    }

    private List<AiChatMessage> toAiContext(List<Message> history, int limit) {
        int fromIndex = Math.max(0, history.size() - limit);
        List<AiChatMessage> context = new ArrayList<>();
        for (Message m : history.subList(fromIndex, history.size())) {
            String role = m.getSender() == Sender.USER ? "user" : "assistant";
            context.add(new AiChatMessage(role, m.getContent()));
        }
        return context;
    }

    private String buildTitle(String content) {
        String trimmed = content.trim();
        if (trimmed.length() <= TITLE_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, TITLE_MAX_LENGTH) + "...";
    }

    private ConversationDetailResponse toDetailResponse(Conversation c) {
        return new ConversationDetailResponse(c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private ConversationSummaryResponse toSummaryResponse(Conversation c) {
        return new ConversationSummaryResponse(c.getId(), c.getTitle(), c.getUpdatedAt());
    }

    private MessageResponse toMessageResponse(Message m) {
        return new MessageResponse(m.getId(), m.getSender(), m.getContent(),
                m.getCorrection(), m.getExplanation(), m.getCreatedAt());
    }
}