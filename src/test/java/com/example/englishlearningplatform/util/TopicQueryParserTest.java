package com.example.englishlearningplatform.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Level;

class TopicQueryParserTest {

    private static final String LEVEL_MESSAGE = "Invalid level value. Valid values are: beginner, intermediate, advanced";
    private static final String STATUS_MESSAGE = "Invalid status value. Valid values are: draft, published, archived";

    // ═══════════ keyword ═══════════

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "     ", "\t", "\n", " \t \n " })
    void keyword_nullEmptyOrBlank_returnsNull(String raw) {
        assertNull(TopicQueryParser.keyword(raw));
    }

    @Test
    void keyword_isTrimmed() {
        assertEquals("grammar", TopicQueryParser.keyword("  grammar \t"));
    }

    @Test
    void keyword_keepsCaseAndInnerSpacing() {
        assertEquals("Present   Simple", TopicQueryParser.keyword("Present   Simple"));
    }

    @Test
    void keyword_of50Characters_isAccepted() {
        String fifty = "a".repeat(50);

        assertEquals(fifty, TopicQueryParser.keyword(fifty));
    }

    @Test
    void keyword_of51Characters_isRejectedWithExistingMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TopicQueryParser.keyword("a".repeat(51)));

        assertEquals("Keyword must not exceed 50 characters", ex.getMessage());
    }

    @Test
    void keyword_lengthIsMeasuredAfterTrim() {
        String paddedFifty = "  " + "a".repeat(50) + "  ";
        String paddedFiftyOne = "  " + "a".repeat(51) + "  ";

        assertEquals("a".repeat(50), TopicQueryParser.keyword(paddedFifty));
        assertThrows(IllegalArgumentException.class, () -> TopicQueryParser.keyword(paddedFiftyOne));
    }

    // ═══════════ level ═══════════

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t", "   \n" })
    void level_nullEmptyOrBlank_returnsNull(String raw) {
        assertNull(TopicQueryParser.level(raw));
    }

    @ParameterizedTest
    @EnumSource(Level.class)
    void level_isCaseInsensitiveForEveryValue(Level expected) {
        String lower = expected.name().toLowerCase(Locale.ROOT);

        assertEquals(expected, TopicQueryParser.level(lower));
        assertEquals(expected, TopicQueryParser.level(expected.name()));
        assertEquals(expected, TopicQueryParser.level(lower.substring(0, 1).toUpperCase(Locale.ROOT)
                + lower.substring(1)));
    }

    @Test
    void level_isTrimmed() {
        assertEquals(Level.INTERMEDIATE, TopicQueryParser.level("  Intermediate  "));
    }

    @ParameterizedTest
    @ValueSource(strings = { "expert", "begin", "beginner,advanced", "1", "beginner;", "BEGINNERR" })
    void level_unknownValue_isRejectedWithExistingMessage(String raw) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TopicQueryParser.level(raw));

        assertEquals(LEVEL_MESSAGE, ex.getMessage());
    }

    @Test
    void level_errorMessageDoesNotEchoTheInput() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TopicQueryParser.level("evil<script>"));

        assertFalse(ex.getMessage().contains("evil"));
    }

    @Test
    void level_errorMessageListsEveryEnumValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TopicQueryParser.level("nope"));

        for (Level level : Level.values()) {
            assertTrue(ex.getMessage().contains(level.name().toLowerCase(Locale.ROOT)),
                    "Message is missing " + level);
        }
    }

    // ═══════════ status ═══════════

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t", "   \n" })
    void status_nullEmptyOrBlank_returnsNull_meaningAllStatuses(String raw) {
        assertNull(TopicQueryParser.status(raw));
    }

    @ParameterizedTest
    @EnumSource(ContentStatus.class)
    void status_isCaseInsensitiveForEveryValue(ContentStatus expected) {
        String lower = expected.name().toLowerCase(Locale.ROOT);

        assertEquals(expected, TopicQueryParser.status(lower));
        assertEquals(expected, TopicQueryParser.status(expected.name()));
        assertEquals(expected, TopicQueryParser.status(lower.substring(0, 1).toUpperCase(Locale.ROOT)
                + lower.substring(1)));
    }

    @Test
    void status_isTrimmed() {
        assertEquals(ContentStatus.PUBLISHED, TopicQueryParser.status("  published "));
    }

    @ParameterizedTest
    @ValueSource(strings = { "deleted", "publish", "draft,published", "0", "ALL" })
    void status_unknownValue_isRejectedWithListedValues(String raw) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TopicQueryParser.status(raw));

        assertEquals(STATUS_MESSAGE, ex.getMessage());
    }

    @Test
    void status_errorMessageListsEveryEnumValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TopicQueryParser.status("nope"));

        for (ContentStatus status : ContentStatus.values()) {
            assertTrue(ex.getMessage().contains(status.name().toLowerCase(Locale.ROOT)),
                    "Message is missing " + status);
        }
    }

    // ═══════════ Locale ═══════════

    @Test
    void parsing_doesNotDependOnTheDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertEquals(Level.INTERMEDIATE, TopicQueryParser.level("intermediate"));
            assertEquals(ContentStatus.PUBLISHED, TopicQueryParser.status("published"));
        } finally {
            Locale.setDefault(original);
        }
    }
}