package com.example.englishlearningplatform.entity;

public enum AchievementCode {

    XP_100("XP Collector", "Earn 100 XP in total"),
    STREAK_7("Week Warrior", "Reach a 7-day streak"),
    QUIZ_PASS_10("Quiz Master", "Pass 10 different quizzes"),
    VOCAB_KNOWN_50("Word Keeper", "Master 50 words"),
    DICTATION_5("Good Listener", "Complete 5 different dictation lessons");

    private final String title;
    private final String description;

    AchievementCode(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}