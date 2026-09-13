package com.example.englishlearningplatform.dto.topic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FlashcardUpdateRequest {

    @NotBlank
    @Size(max = 255)
    private String word;

    @NotBlank
    @Size(max = 500)
    private String meaning;

    @Size(max = 500)
    private String example;
    private String audioUrl;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
}