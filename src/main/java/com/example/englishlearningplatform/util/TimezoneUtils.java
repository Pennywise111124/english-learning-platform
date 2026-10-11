package com.example.englishlearningplatform.util;

import java.time.DateTimeException;
import java.time.ZoneId;

public final class TimezoneUtils {

    public static final String DEFAULT = "Asia/Ho_Chi_Minh";

    private TimezoneUtils() {
    }

    public static String normalizeOrDefault(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT;
        }
        String trimmed = timezone.trim();

        boolean regionLike = trimmed.indexOf('/') >= 0 || trimmed.equals("UTC");
        if (!regionLike || !ZoneId.getAvailableZoneIds().contains(trimmed)) {
            throw new IllegalArgumentException("Invalid timezone");
        }
        return trimmed;
    }

    public static ZoneId zoneOrDefault(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException | NullPointerException e) {
            return ZoneId.of(DEFAULT);
        }
    }
}