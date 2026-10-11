package com.example.englishlearningplatform.dto.leaderboard;

import java.time.LocalDate;
import java.util.List;

public record LeaderboardResponse(LocalDate weekStart, LocalDate weekEnd,
        List<LeaderboardEntryResponse> entries, Me me) {

    public record Me(Integer rank, long xp, boolean visible) {
    }
}
