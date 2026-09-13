package com.example.englishlearningplatform.event;

public class FlashcardChangedEvent {

    // Flashcard luôn thuộc về 1 Topic — cache list flashcard được đánh key
    // theo topicId, nên evict cũng cần đúng topicId này, không phải flashcardId.
    private final Long topicId;

    public FlashcardChangedEvent(Long topicId) {
        this.topicId = topicId;
    }

    public Long getTopicId() {
        return topicId;
    }
}