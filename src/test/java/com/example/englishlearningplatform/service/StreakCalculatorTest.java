package com.example.englishlearningplatform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.example.englishlearningplatform.service.StreakCalculator.State;

class StreakCalculatorTest {

    // Lịch 2026: T2 28/9 và 5/10 · T4 7/10 · T5 8/10 · T6 9/10 · T7 3/10 · CN 4/10
    private static final LocalDate SEP_28 = LocalDate.of(2026, 9, 28);
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_3 = LocalDate.of(2026, 10, 3);
    private static final LocalDate OCT_4 = LocalDate.of(2026, 10, 4);
    private static final LocalDate OCT_5 = LocalDate.of(2026, 10, 5);
    private static final LocalDate OCT_6 = LocalDate.of(2026, 10, 6);
    private static final LocalDate OCT_7 = LocalDate.of(2026, 10, 7);
    private static final LocalDate OCT_8 = LocalDate.of(2026, 10, 8);
    private static final LocalDate OCT_9 = LocalDate.of(2026, 10, 9);

    private static State state(int current, int longest, LocalDate last, LocalDate freezeWeek) {
        return new State(current, longest, last, freezeWeek);
    }

    // ═══════════ onActivity ═══════════

    @Test
    void firstActivity_startsStreakAtOne() {
        assertEquals(state(1, 1, OCT_9, null),
                StreakCalculator.onActivity(state(0, 0, null, null), OCT_9));
    }

    @Test
    void sameDay_keepsState() {
        assertEquals(state(5, 5, OCT_9, null),
                StreakCalculator.onActivity(state(5, 5, OCT_9, null), OCT_9));
    }

    @Test
    void consecutiveDay_incrementsStreakAndLongest() {
        assertEquals(state(6, 6, OCT_9, null),
                StreakCalculator.onActivity(state(5, 5, OCT_8, null), OCT_9));
    }

    @Test
    void consecutiveDay_keepsHigherLongest() {
        assertEquals(state(4, 10, OCT_9, null),
                StreakCalculator.onActivity(state(3, 10, OCT_8, null), OCT_9));
    }

    @Test
    void longGap_resetsStreakButKeepsLongest() {
        assertEquals(state(1, 10, OCT_9, null),
                StreakCalculator.onActivity(state(3, 10, OCT_1, null), OCT_9));
    }

    @Test
    void oneMissedDay_freezeAvailable_savesStreak() {
        assertEquals(state(5, 5, OCT_9, OCT_5),
                StreakCalculator.onActivity(state(4, 4, OCT_7, null), OCT_9));
    }

    @Test
    void oneMissedDay_freezeAlreadyUsedThatWeek_resets() {
        assertEquals(state(1, 4, OCT_9, OCT_5),
                StreakCalculator.onActivity(state(4, 4, OCT_7, OCT_5), OCT_9));
    }

    @Test
    void oneMissedDay_freezeUsedLastWeek_stillAvailableThisWeek() {
        assertEquals(state(5, 5, OCT_9, OCT_5),
                StreakCalculator.onActivity(state(4, 4, OCT_7, SEP_28), OCT_9));
    }

    @Test
    void sundayMissed_freezeBelongsToMissedDaysWeek_notToday() {
        // T7 3/10 -> T2 5/10: ngày bỏ lỡ CN 4/10 thuộc tuần bắt đầu 28/9
        assertEquals(state(5, 5, OCT_5, SEP_28),
                StreakCalculator.onActivity(state(4, 4, OCT_3, null), OCT_5));
    }

    @Test
    void sundayMissed_freezeAlreadyUsedInMissedDaysWeek_resets() {
        assertEquals(state(1, 4, OCT_5, SEP_28),
                StreakCalculator.onActivity(state(4, 4, OCT_3, SEP_28), OCT_5));
    }

    @Test
    void mondayMissed_freezeBelongsToNewWeek() {
        // CN 4/10 -> T3 6/10: ngày bỏ lỡ T2 5/10 thuộc tuần bắt đầu 5/10
        assertEquals(state(5, 5, OCT_6, OCT_5),
                StreakCalculator.onActivity(state(4, 4, OCT_4, null), OCT_6));
    }

    @Test
    void threeDayGap_resetsEvenIfFreezeAvailable() {
        assertEquals(state(1, 4, OCT_9, null),
                StreakCalculator.onActivity(state(4, 4, OCT_6, null), OCT_9));
    }

    @Test
    void dayBeforeLastActive_keepsStateUnchanged() {
        // đổi múi giờ: D < L
        assertEquals(state(4, 4, OCT_9, null),
                StreakCalculator.onActivity(state(4, 4, OCT_9, null), OCT_8));
    }

    // ═══════════ displayStreak (today = 9/10) ═══════════

    @Test
    void displayStreak_activeTodayOrYesterday_returnsCurrent() {
        assertEquals(5, StreakCalculator.displayStreak(state(5, 5, OCT_9, null), OCT_9));
        assertEquals(5, StreakCalculator.displayStreak(state(5, 5, OCT_8, null), OCT_9));
    }

    @Test
    void displayStreak_oneMissedDay_freezeNotUsed_returnsCurrent() {
        assertEquals(5, StreakCalculator.displayStreak(state(5, 5, OCT_7, null), OCT_9));
    }

    @Test
    void displayStreak_oneMissedDay_freezeUsedThatWeek_returnsZero() {
        assertEquals(0, StreakCalculator.displayStreak(state(5, 5, OCT_7, OCT_5), OCT_9));
    }

    @Test
    void displayStreak_twoMissedDays_returnsZero() {
        assertEquals(0, StreakCalculator.displayStreak(state(5, 5, OCT_6, null), OCT_9));
    }

    @Test
    void displayStreak_neverActive_returnsZero() {
        assertEquals(0, StreakCalculator.displayStreak(state(0, 0, null, null), OCT_9));
    }

    // ═══════════ freezeAvailable (today = 9/10) ═══════════

    @Test
    void freezeAvailable_usedThisWeek_isFalse() {
        assertFalse(StreakCalculator.freezeAvailable(state(1, 1, OCT_8, OCT_5), OCT_9));
    }

    @Test
    void freezeAvailable_usedLastWeek_isTrue() {
        assertTrue(StreakCalculator.freezeAvailable(state(1, 1, OCT_8, SEP_28), OCT_9));
    }

    @Test
    void freezeAvailable_neverUsed_isTrue() {
        assertTrue(StreakCalculator.freezeAvailable(state(1, 1, OCT_8, null), OCT_9));
    }
}