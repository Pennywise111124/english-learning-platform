package com.example.englishlearningplatform.util;

import java.util.Locale;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Level;

public final class TopicQueryParser {
    public static final int MAX_KEYWORD_LENGTH = 50;

    private TopicQueryParser() {
    }

    public static String keyword(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("Keyword must not exceed " + MAX_KEYWORD_LENGTH + " characters");
        }
        return trimmed;
    }

    public static Level level(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Level.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid level value. Valid values are: beginner, intermediate, advanced");
        }
    }

    public static ContentStatus status(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ContentStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid status value. Valid values are: draft, published, archived");
        }
    }
}