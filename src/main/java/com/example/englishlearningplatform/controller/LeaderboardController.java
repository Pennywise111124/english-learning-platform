package com.example.englishlearningplatform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.englishlearningplatform.dto.leaderboard.LeaderboardResponse;
import com.example.englishlearningplatform.service.LeaderboardService;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/weekly")
    public ResponseEntity<LeaderboardResponse> weekly(Authentication authentication) {
        return ResponseEntity.ok(leaderboardService.getWeekly(authentication.getName()));
    }
}