package com.example.englishlearningplatform.dto.dictation;

import java.util.Locale;

public enum DictationSort {
    NEWEST, TITLE, RECENT;

    public static DictationSort from(String value) {
        if (value == null || value.isBlank()) {
            return NEWEST;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid sort: " + value + " (allowed: newest, title, recent)");
        }
    }
}