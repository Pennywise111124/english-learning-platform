package com.example.englishlearningplatform.dto.topic;

import com.example.englishlearningplatform.entity.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TopicCreateRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 500)
    private String description;

    @NotNull
    private Level level;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }
}