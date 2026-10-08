package com.example.englishlearningplatform.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.englishlearningplatform.dto.dictation.DictationWordResult;
import com.example.englishlearningplatform.dto.dictation.WordStatus;
import com.example.englishlearningplatform.service.DictationComparator.ComparisonResult;

class DictationComparatorTest {

    // ------------------------------------------------------------------
    // tokenize()
    // ------------------------------------------------------------------

    @Test
    void tokenize_shouldLowercaseAndStripPunctuation() {
        assertEquals(List.of("hello", "world"), DictationComparator.tokenize("Hello, World!"));
    }

    @Test
    void tokenize_curlyApostropheShouldBeNormalized() {
        assertEquals(List.of("don't", "stop"), DictationComparator.tokenize("Don\u2019t stop"));
    }

    @Test
    void tokenize_hyphenShouldSplitWords() {
        assertEquals(List.of("well", "known"), DictationComparator.tokenize("well-known"));
    }

    @Test
    void tokenize_quotesAroundWordShouldBeStripped() {
        assertEquals(List.of("hello"), DictationComparator.tokenize("'hello'"));
        assertEquals(List.of("don't"), DictationComparator.tokenize("don't"));
    }

    @Test
    void tokenize_blankOrOnlyPunctuationShouldReturnEmptyList() {
        assertEquals(List.of(), DictationComparator.tokenize(null));
        assertEquals(List.of(), DictationComparator.tokenize("   "));
        assertEquals(List.of(), DictationComparator.tokenize("?!..."));
    }

    @Test
    void tokenize_multipleSpacesAndNewlinesShouldCollapse() {
        assertEquals(List.of("a", "b", "c"), DictationComparator.tokenize("a   b\n c"));
    }

    @Test
    void tokenize_punctuationBetweenWordsShouldSplit() {
        assertEquals(List.of("hello", "world"), DictationComparator.tokenize("hello,world"));
        assertEquals(List.of("end", "start"), DictationComparator.tokenize("end.Start"));
        assertEquals(List.of("a", "b"), DictationComparator.tokenize("a/b"));
    }

    @Test
    void tokenize_apostropheInsideWordShouldStay() {
        assertEquals(List.of("don't", "stop"), DictationComparator.tokenize("don't,stop"));
    }

    // Khoá giới hạn đã chấp nhận: dấu đứng giữa hai chữ số cũng tách từ.
    @Test
    void tokenize_knownLimitation_separatorsInsideNumbersAndAbbreviationsSplit() {
        assertEquals(List.of("1", "000"), DictationComparator.tokenize("1,000"));
        assertEquals(List.of("3", "5"), DictationComparator.tokenize("3.5"));
        assertEquals(List.of("u", "s", "a"), DictationComparator.tokenize("U.S.A."));
    }

    // ------------------------------------------------------------------
    // compare()
    // ------------------------------------------------------------------

    @Test
    void compare_identicalText_shouldBe100AndAllCorrect() {
        ComparisonResult res = DictationComparator.compare("I like cats", "I like cats");
        assertEquals(100.0, res.accuracy());
        assertEquals(3, res.words().size());
        assertTrue(res.words().stream().allMatch(w -> w.status() == WordStatus.CORRECT));
    }

    @Test
    void compare_caseAndPunctuationIgnored_shouldBe100() {
        ComparisonResult res = DictationComparator.compare("I like cats.", "i LIKE cats");
        assertEquals(100.0, res.accuracy());
        assertEquals(3, res.words().size());
        assertTrue(res.words().stream().allMatch(w -> w.status() == WordStatus.CORRECT));
    }

    @Test
    void compare_oneSubstitution_shouldMarkWrongWithExpected() {
        ComparisonResult res = DictationComparator.compare("I like cats", "I like dogs");
        assertEquals(66.7, res.accuracy(), 0.0001);

        List<DictationWordResult> words = res.words();
        assertEquals(3, words.size());
        assertEquals(new DictationWordResult("i", WordStatus.CORRECT, null), words.get(0));
        assertEquals(new DictationWordResult("like", WordStatus.CORRECT, null), words.get(1));
        assertEquals(new DictationWordResult("dogs", WordStatus.WRONG, "cats"), words.get(2));
    }

    @Test
    void compare_missingWord_shouldMarkMissing() {
        ComparisonResult res = DictationComparator.compare("I like big cats", "I like cats");
        assertEquals(75.0, res.accuracy(), 0.0001);

        List<DictationWordResult> words = res.words();
        assertEquals(4, words.size());
        assertEquals(new DictationWordResult("i", WordStatus.CORRECT, null), words.get(0));
        assertEquals(new DictationWordResult("like", WordStatus.CORRECT, null), words.get(1));
        assertEquals(new DictationWordResult("big", WordStatus.MISSING, null), words.get(2));
        assertEquals(new DictationWordResult("cats", WordStatus.CORRECT, null), words.get(3));
    }

    @Test
    void compare_extraWord_shouldMarkExtra() {
        ComparisonResult res = DictationComparator.compare("I like cats", "I really like cats");
        assertEquals(75.0, res.accuracy(), 0.0001);

        List<DictationWordResult> words = res.words();
        assertEquals(4, words.size());
        assertEquals(new DictationWordResult("i", WordStatus.CORRECT, null), words.get(0));
        assertEquals(new DictationWordResult("really", WordStatus.EXTRA, null), words.get(1));
        assertEquals(new DictationWordResult("like", WordStatus.CORRECT, null), words.get(2));
        assertEquals(new DictationWordResult("cats", WordStatus.CORRECT, null), words.get(3));
    }

    @Test
    void compare_gapWithUnequalSizes_shouldPairWrongThenExtra() {
        ComparisonResult res = DictationComparator.compare("a b c d", "a x y z d");
        assertEquals(40.0, res.accuracy(), 0.0001);

        List<DictationWordResult> words = res.words();
        assertEquals(5, words.size());
        assertEquals(new DictationWordResult("a", WordStatus.CORRECT, null), words.get(0));
        assertEquals(new DictationWordResult("x", WordStatus.WRONG, "b"), words.get(1));
        assertEquals(new DictationWordResult("y", WordStatus.WRONG, "c"), words.get(2));
        assertEquals(new DictationWordResult("z", WordStatus.EXTRA, null), words.get(3));
        assertEquals(new DictationWordResult("d", WordStatus.CORRECT, null), words.get(4));
    }

    @Test
    void compare_completelyDifferent_shouldBeZero() {
        ComparisonResult res = DictationComparator.compare("a b", "c d");
        assertEquals(0.0, res.accuracy(), 0.0001);

        List<DictationWordResult> words = res.words();
        assertEquals(2, words.size());
        assertEquals(new DictationWordResult("c", WordStatus.WRONG, "a"), words.get(0));
        assertEquals(new DictationWordResult("d", WordStatus.WRONG, "b"), words.get(1));
    }

    @Test
    void compare_roundingShouldKeepOneDecimal() {
        ComparisonResult res = DictationComparator.compare("a b c", "a b");
        assertEquals(66.7, res.accuracy(), 0.0001);
    }

    @Test
    void compare_whenUserInputHasNoWords_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> DictationComparator.compare("I like cats", "?!..."));
    }

    @Test
    void compare_whenTranscriptHasNoWords_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> DictationComparator.compare("?!...", "I like cats"));
    }

    @Test
    void compare_whenUserInputExceedsMaxWords_shouldThrowIllegalArgumentException() {
        String transcript = "a ".repeat(1000);
        String userInputExceeded = "a ".repeat(1001);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DictationComparator.compare(transcript, userInputExceeded));

        assertEquals("Text must not exceed " + DictationComparator.MAX_WORDS + " words", exception.getMessage());
    }

    @Test
    void compare_whenExactlyMaxWords_shouldNotThrow() {
        String transcript = "a ".repeat(1000);
        String userInputMax = "a ".repeat(1000);

        assertDoesNotThrow(() -> DictationComparator.compare(transcript, userInputMax));
    }

    @Test
    void compare_tieBreak_shouldPreferDroppingTranscriptWordFirst() {
        ComparisonResult res = DictationComparator.compare("a b", "b a");
        assertEquals(50.0, res.accuracy(), 0.0001);
        assertEquals(new DictationWordResult("a", WordStatus.MISSING, null), res.words().get(0));
        assertEquals(new DictationWordResult("b", WordStatus.CORRECT, null), res.words().get(1));
        assertEquals(new DictationWordResult("a", WordStatus.EXTRA, null), res.words().get(2));
    }

    @Test
    void compare_missingSpaceAfterComma_shouldStillBe100() {
        ComparisonResult res = DictationComparator.compare("Hello, world", "hello,world");
        assertEquals(100.0, res.accuracy(), 0.0001);
    }

    // Giới hạn 1000 từ đếm SAU chuẩn hoá: "a," lặp 1001 lần là 1001 từ.
    @Test
    void compare_wordLimitCountsAfterNormalization() {
        String transcript = "a ".repeat(10);
        assertThrows(IllegalArgumentException.class,
                () -> DictationComparator.compare(transcript, "a,".repeat(1001)));
        assertDoesNotThrow(() -> DictationComparator.compare(transcript, "a,".repeat(1000)));
    }
}