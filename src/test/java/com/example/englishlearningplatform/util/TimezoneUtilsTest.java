package com.example.englishlearningplatform.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TimezoneUtilsTest {

    @Test
    void null_returnsDefault() {
        assertEquals(TimezoneUtils.DEFAULT, TimezoneUtils.normalizeOrDefault(null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "   " })
    void blank_returnsDefault(String input) {
        assertEquals(TimezoneUtils.DEFAULT, TimezoneUtils.normalizeOrDefault(input));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Asia/Tokyo", "UTC", "America/Los_Angeles", "Asia/Ho_Chi_Minh" })
    void validZone_isReturnedUnchanged(String zone) {
        assertEquals(zone, TimezoneUtils.normalizeOrDefault(zone));
    }

    @Test
    void surroundingWhitespace_isTrimmed() {
        assertEquals("Asia/Tokyo", TimezoneUtils.normalizeOrDefault("  Asia/Tokyo  "));
    }

    @ParameterizedTest
    @ValueSource(strings = { "+05:00", "Mars/Base", "EST", "GMT+7", "asia/tokyo", "GMT", "EST5EDT" })
    void invalidZone_throwsIllegalArgument(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TimezoneUtils.normalizeOrDefault(input));
        assertEquals("Invalid timezone", ex.getMessage());
    }
}