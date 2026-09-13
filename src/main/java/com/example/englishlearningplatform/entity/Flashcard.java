package com.example.englishlearningplatform.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "flashcards")
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false, length = 500)
    private String meaning;

    @Column(columnDefinition = "TEXT")
    private String example;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // URL external do Admin tự nhập (VD: link audio phát âm có sẵn) — KHÔNG phải
    // file do hệ thống tự lưu trữ, FR-6 chỉ áp dụng cho ảnh (đã chốt mục 4)
    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    // ── Getters & Setters ──────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
}