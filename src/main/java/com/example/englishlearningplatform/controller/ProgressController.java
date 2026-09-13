package com.example.englishlearningplatform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.englishlearningplatform.dto.progress.UserProgressResponse;
import com.example.englishlearningplatform.service.UserProgressService;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
public class ProgressController {

    private final UserProgressService userProgressService;

    public ProgressController(UserProgressService userProgressService) {
        this.userProgressService = userProgressService;
    }

    @GetMapping("/progress")
    public ResponseEntity<List<UserProgressResponse>> getMyProgress() {
        return ResponseEntity.ok(userProgressService.getMyProgress());
    }
}