package com.example.englishlearningplatform.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.englishlearningplatform.dto.quiz.QuizRequest;
import com.example.englishlearningplatform.dto.quiz.QuizSummaryResponse;
import com.example.englishlearningplatform.service.QuizService;

@RestController
@RequestMapping("/api/admin")
public class AdminQuizController {

    private final QuizService quizService;

    public AdminQuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/topics/{topicId}/quizzes")
    public ResponseEntity<QuizSummaryResponse> createQuiz(
            @PathVariable Long topicId, @Valid @RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.createQuiz(topicId, request));
    }

    @PutMapping("/quizzes/{id}")
    public ResponseEntity<QuizSummaryResponse> updateQuiz(
            @PathVariable Long id, @Valid @RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.updateQuiz(id, request));
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.noContent().build();
    }
}