package com.example.englishlearningplatform.dto.leaderboard;

public record LeaderboardEntryResponse(int rank, String username, long xp, boolean me) {
}
