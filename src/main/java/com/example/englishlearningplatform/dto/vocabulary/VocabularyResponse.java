package com.example.englishlearningplatform.dto.vocabulary;

import java.time.Instant;

import com.example.englishlearningplatform.entity.Flashcard;
import com.example.englishlearningplatform.entity.UserVocabulary;
import com.example.englishlearningplatform.entity.VocabularyStatus;

public class VocabularyResponse {

    private Long id;
    private Long flashcardId;
    private String word;
    private String meaning;
    private String example;
    private String imageUrl;
    private String audioUrl;
    private VocabularyStatus status;
    private Integer reviewCount;
    private Integer difficulty;
    private Instant nextReviewAt;
    private Instant lastReviewedAt;

    public static VocabularyResponse from(UserVocabulary userVocab) {
        VocabularyResponse dto = new VocabularyResponse();
        dto.id = userVocab.getId();

        Flashcard flashcard = userVocab.getFlashcard();
        if (flashcard != null) {
            dto.flashcardId = flashcard.getId();
            dto.word = flashcard.getWord();
            dto.meaning = flashcard.getMeaning();
            dto.example = flashcard.getExample();
            dto.imageUrl = flashcard.getImageUrl();
            dto.audioUrl = flashcard.getAudioUrl();
        }

        dto.status = userVocab.getStatus();
        dto.reviewCount = userVocab.getReviewCount();
        dto.difficulty = userVocab.getDifficulty();
        dto.nextReviewAt = userVocab.getNextReviewAt();
        dto.lastReviewedAt = userVocab.getLastReviewedAt();

        return dto;
    }

    public Long getId() {
        return id;
    }

    public Long getFlashcardId() {
        return flashcardId;
    }

    public String getWord() {
        return word;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getExample() {
        return example;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public VocabularyStatus getStatus() {
        return status;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public Instant getNextReviewAt() {
        return nextReviewAt;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }
}
