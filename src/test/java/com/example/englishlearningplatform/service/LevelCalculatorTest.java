package com.example.englishlearningplatform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LevelCalculatorTest {

    @ParameterizedTest(name = "xp={0} -> level {1}, còn {2}")
    @CsvSource({
            "0,   1, 100",
            "99,  1, 1",
            "100, 2, 300",
            "399, 2, 1",
            "400, 3, 500",
            "900, 4, 700"
    })
    void levelAndXpToNext(long xp, int expectedLevel, long expectedToNext) {
        assertEquals(expectedLevel, LevelCalculator.level(xp));
        assertEquals(expectedToNext, LevelCalculator.xpToNextLevel(xp));
    }

    @Test
    void negativeXp_isTreatedAsZero() {
        assertEquals(1, LevelCalculator.level(-5));
        assertEquals(100, LevelCalculator.xpToNextLevel(-5));
    }

    @Test
    void largeXp_doesNotOverflowOrLosePrecision() {
        // 100 * 1_000_000^2 = 1e14: level phải đúng ở biên
        assertEquals(1_000_001, LevelCalculator.level(100_000_000_000_000L));
        assertEquals(1_000_000, LevelCalculator.level(100_000_000_000_000L - 1));
    }
}