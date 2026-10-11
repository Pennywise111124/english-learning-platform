package com.example.englishlearningplatform.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "user_stats")
public class UserStats {

    @Id
    @Column(name = "user_id")
    private Long userId; // KHÔNG @GeneratedValue: id chính là id người dùng

    @Column(name = "total_xp", nullable = false)
    private long totalXp;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Column(name = "freeze_used_week")
    private LocalDate freezeUsedWeek; // ngày thứ Hai của tuần đã dùng đóng băng

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public long getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(long totalXp) {
        this.totalXp = totalXp;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public LocalDate getLastActiveDate() {
        return lastActiveDate;
    }

    public void setLastActiveDate(LocalDate lastActiveDate) {
        this.lastActiveDate = lastActiveDate;
    }

    public LocalDate getFreezeUsedWeek() {
        return freezeUsedWeek;
    }

    public void setFreezeUsedWeek(LocalDate freezeUsedWeek) {
        this.freezeUsedWeek = freezeUsedWeek;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}