package com.example.englishlearningplatform.service;

public final class LevelCalculator {

    private static final long XP_PER_STEP = 100;

    private LevelCalculator() {
    }

    public static int level(long totalXp) {
        long steps = Math.max(0, totalXp) / XP_PER_STEP;
        return (int) isqrt(steps) + 1;
    }

    public static long xpToNextLevel(long totalXp) {
        long xp = Math.max(0, totalXp);
        long level = level(xp);
        return XP_PER_STEP * level * level - xp;
    }

    private static long isqrt(long n) {
        long s = (long) Math.sqrt((double) n);
        while (s * s > n) {
            s--;
        }
        while ((s + 1) * (s + 1) <= n) {
            s++;
        }
        return s;
    }
}