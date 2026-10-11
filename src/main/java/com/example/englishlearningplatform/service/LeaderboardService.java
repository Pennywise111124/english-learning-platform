package com.example.englishlearningplatform.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.englishlearningplatform.dto.leaderboard.LeaderboardEntryResponse;
import com.example.englishlearningplatform.dto.leaderboard.LeaderboardResponse;
import com.example.englishlearningplatform.dto.leaderboard.LeaderboardRow;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.repository.XpEventRepository;
import com.example.englishlearningplatform.util.TimezoneUtils;

@Service
public class LeaderboardService {

    static final int TOP_SIZE = 20;

    private final UserRepository userRepository;
    private final XpEventRepository xpEventRepository;
    private final Clock clock;

    public LeaderboardService(UserRepository userRepository, XpEventRepository xpEventRepository, Clock clock) {
        this.userRepository = userRepository;
        this.xpEventRepository = xpEventRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getWeekly(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        ZoneId zone = TimezoneUtils.zoneOrDefault(user.getTimezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate from = StreakCalculator.weekStart(today);
        LocalDate to = from.plusDays(6);

        List<LeaderboardRow> rows = xpEventRepository.findWeeklyTop(from, to, TOP_SIZE);
        List<LeaderboardEntryResponse> entries = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardRow row = rows.get(i);
            entries.add(new LeaderboardEntryResponse(i + 1, row.getUsername(), row.getXp(),
                    row.getUserId().equals(user.getId())));
        }

        long myXp = xpEventRepository.sumXpBetween(user.getId(), from, to);
        Integer myRank = myXp > 0
                ? (int) (xpEventRepository.countRankedAhead(user.getId(), myXp, from, to) + 1)
                : null;

        return new LeaderboardResponse(from, to, entries,
                new LeaderboardResponse.Me(myRank, myXp, user.isLeaderboardVisible()));
    }
}