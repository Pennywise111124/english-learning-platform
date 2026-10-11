package com.example.englishlearningplatform.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "user_achievements")
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AchievementCode code;

    @Column(name = "awarded_at", nullable = false)
    private Instant awardedAt;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public AchievementCode getCode() {
        return code;
    }

    public void setCode(AchievementCode code) {
        this.code = code;
    }

    public Instant getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(Instant awardedAt) {
        this.awardedAt = awardedAt;
    }

    public Instant getDismissedAt() {
        return dismissedAt;
    }

    public void setDismissedAt(Instant dismissedAt) {
        this.dismissedAt = dismissedAt;
    }
}