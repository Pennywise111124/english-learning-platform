package com.example.englishlearningplatform.entity;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "dictation_results")
public class DictationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private DictationLesson lesson;

    @Column(name = "user_input", nullable = false, columnDefinition = "TEXT")
    private String userInput;

    @Column(nullable = false)
    private Double accuracy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // Service gán bằng clock.instant(), không khởi tạo mặc định

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DictationLesson getLesson() {
        return lesson;
    }

    public void setLesson(DictationLesson lesson) {
        this.lesson = lesson;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}