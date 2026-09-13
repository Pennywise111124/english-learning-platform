package com.example.englishlearningplatform.dto.progress;

import com.example.englishlearningplatform.entity.ProgressStatus;
import com.example.englishlearningplatform.entity.UserProgress;

// Khớp shape mảng đã chốt ở FE_Handoff_Brief.md — LƯU Ý: brief ghi field tên
// "topicId"/"topicTitle" (không phải nested object "topic": {...}) — cần lấy
// topic.getTitle() ra rồi gán phẳng vào field riêng, không trả thẳng entity Topic lồng bên trong
public class UserProgressResponse {

    private Long topicId;
    private String topicTitle;
    private ProgressStatus status;
    private int progressPercent;
    private java.time.Instant updatedAt;

    public static UserProgressResponse from(UserProgress progress) {
        UserProgressResponse dto = new UserProgressResponse();
        dto.topicId = progress.getTopic().getId();
        dto.topicTitle = progress.getTopic().getTitle();
        dto.status = progress.getStatus();
        dto.progressPercent = progress.getProgressPercent();
        dto.updatedAt = progress.getUpdatedAt();
        return dto;
    }

    public Long getTopicId() {
        return topicId;
    }

    public String getTopicTitle() {
        return topicTitle;
    }

    public ProgressStatus getStatus() {
        return status;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public java.time.Instant getUpdatedAt() {
        return updatedAt;
    }
}