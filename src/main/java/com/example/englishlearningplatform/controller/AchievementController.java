package com.example.englishlearningplatform.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.englishlearningplatform.dto.achievement.AchievementResponse;
import com.example.englishlearningplatform.service.AchievementService;

@RestController
@RequestMapping("/api/users/me/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    public ResponseEntity<List<AchievementResponse>> getMyAchievements(Authentication authentication) {
        return ResponseEntity.ok(achievementService.getMyAchievements(authentication.getName()));
    }

    @PostMapping("/{code}/dismiss")
    public ResponseEntity<Void> dismiss(@PathVariable String code, Authentication authentication) {
        achievementService.dismiss(authentication.getName(), code);
        return ResponseEntity.noContent().build();
    }
}