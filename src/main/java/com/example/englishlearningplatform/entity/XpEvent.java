package com.example.englishlearningplatform.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "xp_events")
public class XpEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // cố ý không dùng @ManyToOne: sổ cái chỉ cần id, tránh nạp User

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private XpType type;

    @Column(nullable = false)
    private int xp;

    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    @Column(name = "dedupe_key", nullable = false, length = 100)
    private String dedupeKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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

    public XpType getType() {
        return type;
    }

    public void setType(XpType type) {
        this.type = type;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public void setDedupeKey(String dedupeKey) {
        this.dedupeKey = dedupeKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}