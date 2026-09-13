package com.example.englishlearningplatform.dto.topic;

import com.example.englishlearningplatform.entity.Flashcard;

public class FlashcardResponse {

    private Long id;
    private String word;
    private String meaning;
    private String example;
    private String imageUrl;
    private String audioUrl;

    public static FlashcardResponse from(Flashcard flashcard) {
        FlashcardResponse dto = new FlashcardResponse();
        dto.id = flashcard.getId();
        dto.word = flashcard.getWord();
        dto.meaning = flashcard.getMeaning();
        dto.example = flashcard.getExample();
        dto.imageUrl = flashcard.getImageUrl();
        dto.audioUrl = flashcard.getAudioUrl();
        return dto;
    }

    public Long getId() {
        return id;
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
}