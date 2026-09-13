package com.example.englishlearningplatform.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // LƯU Ý: response ở đây dùng QuizQuestionPublicResponse (không có
    // correctAnswer)
    // dù đây là API dành cho Admin — vì Admin vừa nhập correctAnswer xong (đã có
    // sẵn trong request), không cần Backend echo lại. Giữ 1 loại response DTO
    // duy nhất cho Question đơn giản hơn phải tạo thêm AdminQuizQuestionResponse
    // chỉ để hiện correctAnswer trong 1 lần xác nhận.
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