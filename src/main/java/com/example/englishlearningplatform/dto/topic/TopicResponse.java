package com.example.englishlearningplatform.dto.topic;

import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;

public class TopicResponse {

    private Long id;
    private String title;
    private String description;
    private Level level;
    private String imageUrl;

    // Factory method, theo đúng convention UserResponse.from(user) đã dùng ở
    // AuthService
    public static TopicResponse from(Topic topic) {
        TopicResponse dto = new TopicResponse();
        dto.id = topic.getId();
        dto.title = topic.getTitle();
        dto.description = topic.getDescription();
        dto.level = topic.getLevel();
        dto.imageUrl = topic.getImageUrl();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Level getLevel() {
        return level;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}