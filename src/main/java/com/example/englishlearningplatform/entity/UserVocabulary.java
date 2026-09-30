package com.example.englishlearningplatform.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_vocabulary", uniqueConstraints = @UniqueConstraint(name = "uq_user_vocabulary_user_flashcard", columnNames = {
        "user_id", "flashcard_id" }))
public class UserVocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VocabularyStatus status;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    // Số lần bị đánh "chưa nhớ" — dùng để sắp xếp ưu tiên (FR-8.5)
    @Column(nullable = false)
    private Integer difficulty = 0;

    // Bậc hiện tại trên thang 1/3/7/14/30 ngày (0..4)
    @Column(name = "interval_level", nullable = false)
    private Integer intervalLevel = 0;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "next_review_at", nullable = false)
    private Instant nextReviewAt;

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

    public Flashcard getFlashcard() {
        return flashcard;
    }

    public void setFlashcard(Flashcard flashcard) {
        this.flashcard = flashcard;
    }

    public VocabularyStatus getStatus() {
        return status;
    }

    public void setStatus(VocabularyStatus status) {
        this.status = status;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getIntervalLevel() {
        return intervalLevel;
    }

    public void setIntervalLevel(Integer intervalLevel) {
        this.intervalLevel = intervalLevel;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(Instant lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public Instant getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(Instant nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }

}