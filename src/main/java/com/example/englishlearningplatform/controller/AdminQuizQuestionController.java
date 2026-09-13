package com.example.englishlearningplatform.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.englishlearningplatform.dto.quiz.AdminQuizQuestionResponse;
import com.example.englishlearningplatform.dto.quiz.QuizQuestionPublicResponse;
import com.example.englishlearningplatform.dto.quiz.QuizQuestionRequest;
import com.example.englishlearningplatform.service.QuizService;

@RestController
@RequestMapping("/api/admin")
public class AdminQuizQuestionController {

    private final QuizService quizService;

    public AdminQuizQuestionController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/quizzes/{quizId}/questions")
    public ResponseEntity<List<AdminQuizQuestionResponse>> getQuestionsForAdmin(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.getQuestionsForAdmin(quizId));
    }

    @PostMapping("/quizzes/{quizId}/questions")
    public ResponseEntity<QuizQuestionPublicResponse> createQuestion(
            @PathVariable Long quizId, @Valid @RequestBody QuizQuestionRequest request) {
        return ResponseEntity.ok(quizService.createQuestion(quizId, request));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<QuizQuestionPublicResponse> updateQuestion(
            @PathVariable Long id, @Valid @RequestBody QuizQuestionRequest request) {
        return ResponseEntity.ok(quizService.updateQuestion(id, request));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        quizService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}