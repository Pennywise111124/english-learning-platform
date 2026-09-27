package com.example.englishlearningplatform.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.englishlearningplatform.dto.auth.ChangePasswordRequest;
import com.example.englishlearningplatform.dto.auth.UserResponse;
import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.dto.quiz.QuizAttemptResponse;
import com.example.englishlearningplatform.service.QuizAttemptService;
import com.example.englishlearningplatform.service.UserService;
import com.example.englishlearningplatform.util.PaginationUtils;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;
    private final QuizAttemptService quizAttemptService;

    public UserController(UserService userService, QuizAttemptService quizAttemptService) {
        this.userService = userService;
        this.quizAttemptService = quizAttemptService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @PostMapping("/avatar")
    public ResponseEntity<UserResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateMyAvatar(file));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/quiz-attempts")
    public ResponseEntity<PageResponse<QuizAttemptResponse>> getMyRecentAttempts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginationUtils.validate(page, size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageResponse.from(quizAttemptService.getMyRecentAttempts(pageable)));
    }
}