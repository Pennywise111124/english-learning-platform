package com.example.englishlearningplatform.dto.topic;

public enum TopicSort {
    NEWEST, POPULAR, TITLE;

    // null/blank → NEWEST (mặc định). Giá trị lạ → 400, KHÔNG âm thầm rơi về mặc
    // định.
    public static TopicSort from(String value) {
        if (value == null || value.isBlank()) {
            return NEWEST;
        }
        for (TopicSort s : values()) {
            if (s.name().equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid sort value. Valid values are: newest, popular, title");
    }
}