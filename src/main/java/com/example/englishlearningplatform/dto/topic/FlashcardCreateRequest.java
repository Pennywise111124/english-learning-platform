package com.example.englishlearningplatform.dto.topic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FlashcardCreateRequest {

    // Không có field topicId ở đây — topic lấy từ path variable
    // (POST /api/admin/topics/{id}/flashcards), không phải từ body,
    // tránh trường hợp body tự khai topicId khác với path (dữ liệu client gửi
    // không đáng tin, giống nguyên tắc FR-2.7/mục 2.2 dù đây không phải ownership
    // user)

    @NotBlank
    @Size(max = 255)
    private String word;

    @NotBlank
    @Size(max = 500)
    private String meaning;

    @Size(max = 500)
    private String example;

    // URL external do Admin tự nhập (đã chốt mục 4) — khác imageUrl, KHÔNG qua FR-6
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