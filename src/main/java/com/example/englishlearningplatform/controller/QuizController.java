package com.example.englishlearningplatform.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.dto.quiz.*;
import com.example.englishlearningplatform.service.QuizAttemptService;
import com.example.englishlearningplatform.service.QuizService;
import com.example.englishlearningplatform.util.PaginationUtils;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;

    public QuizController(QuizService quizService, QuizAttemptService quizAttemptService) {
        this.quizService = quizService;
        this.quizAttemptService = quizAttemptService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizDetailResponse> getQuizDetail(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizDetail(id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<QuizResultResponse> submit(
            @PathVariable Long id, @Valid @RequestBody SubmitQuizRequest request) {
        return ResponseEntity.ok(quizAttemptService.submitQuiz(id, request));
    }

    @GetMapping("/{id}/attempts")
    public ResponseEntity<PageResponse<QuizAttemptResponse>> getAttempts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginationUtils.validate(page, size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageResponse.from(quizAttemptService.getMyAttempts(id, pageable)));
    }
}