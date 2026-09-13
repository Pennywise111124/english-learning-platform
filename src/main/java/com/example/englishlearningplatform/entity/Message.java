package com.example.englishlearningplatform.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false, length = 10)
    private Sender sender;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "correction", columnDefinition = "TEXT")
    private String correction;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Message() {
        // JPA
    }

    private Message(Conversation conversation, Sender sender, String content,
            String correction, String explanation) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
        this.correction = correction;
        this.explanation = explanation;
        this.createdAt = Instant.now();
    }

    public static Message ofUser(Conversation conversation, String content) {
        return new Message(conversation, Sender.USER, content, null, null);
    }

    public static Message ofAi(Conversation conversation, String content, String correction, String explanation) {
        return new Message(conversation, Sender.AI, content, correction, explanation);
    }

    public Long getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public Sender getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getCorrection() {
        return correction;
    }

    public String getExplanation() {
        return explanation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}