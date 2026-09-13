package com.example.englishlearningplatform.util;

public final class PaginationUtils {

    public static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
    }

    public static void validate(int page, int size) {
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size không được vượt quá: " + MAX_PAGE_SIZE);
        }
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Page index phải >= 0 và size phải >= 1.");
        }
    }
}