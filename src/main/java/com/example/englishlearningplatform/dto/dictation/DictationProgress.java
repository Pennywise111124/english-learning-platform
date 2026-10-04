package com.example.englishlearningplatform.dto.dictation;

import java.util.Locale;

public enum DictationProgress {
    NEW, PRACTICED;

    public static DictationProgress from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid progress: " + value + " (allowed: new, practiced)");
        }
    }
}