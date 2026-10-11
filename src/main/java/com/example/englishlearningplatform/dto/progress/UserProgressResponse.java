package com.example.englishlearningplatform.dto.progress;

import java.time.Instant;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.ProgressStatus;
import com.example.englishlearningplatform.entity.UserProgress;

public class UserProgressResponse {

    private Long topicId;
    private String topicTitle;
    private ProgressStatus status;
    private int progressPercent;
    private Instant updatedAt;
    private boolean topicAvailable;

    public static UserProgressResponse from(UserProgress progress) {
        UserProgressResponse dto = new UserProgressResponse();
        dto.topicId = progress.getTopic().getId();
        dto.topicTitle = progress.getTopic().getTitle();
        dto.status = progress.getStatus();
        dto.progressPercent = progress.getProgressPercent();
        dto.updatedAt = progress.getUpdatedAt();
        dto.topicAvailable = progress.getTopic().getStatus() == ContentStatus.PUBLISHED;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isTopicAvailable() {
        return topicAvailable;
    }
}