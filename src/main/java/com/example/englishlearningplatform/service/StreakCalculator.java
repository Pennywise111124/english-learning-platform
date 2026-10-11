package com.example.englishlearningplatform.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public final class StreakCalculator {

    private StreakCalculator() {
    }

    public record State(int currentStreak, int longestStreak,
            LocalDate lastActiveDate, LocalDate freezeUsedWeek) {
    }

    static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static State onActivity(State s, LocalDate day) {
        LocalDate last = s.lastActiveDate();
        LocalDate freezeWeek = s.freezeUsedWeek();
        int current = s.currentStreak();

        if (last == null) {
            current = 1;
            last = day;
        } else {
            long gap = ChronoUnit.DAYS.between(last, day);
            if (gap >= 1) {
                if (gap == 1) {
                    current++;
                } else if (gap == 2) {
                    LocalDate missedWeek = weekStart(last.plusDays(1));
                    if (!missedWeek.equals(freezeWeek)) {
                        current++;
                        freezeWeek = missedWeek;
                    } else {
                        current = 1;
                    }
                } else {
                    current = 1;
                }
                last = day;
            }
        }

        int longest = Math.max(s.longestStreak(), current);
        return new State(current, longest, last, freezeWeek);
    }

    public static int displayStreak(State s, LocalDate today) {
        LocalDate last = s.lastActiveDate();
        if (last == null) {
            return 0;
        }
        long gap = ChronoUnit.DAYS.between(last, today);
        if (gap <= 1) {
            return s.currentStreak();
        }
        if (gap == 2 && !weekStart(last.plusDays(1)).equals(s.freezeUsedWeek())) {
            return s.currentStreak();
        }
        return 0;
    }

    public static boolean freezeAvailable(State s, LocalDate today) {
        return !weekStart(today).equals(s.freezeUsedWeek());
    }
}