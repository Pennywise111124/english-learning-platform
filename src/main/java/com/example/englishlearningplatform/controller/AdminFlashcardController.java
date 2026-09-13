package com.example.englishlearningplatform.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.englishlearningplatform.dto.topic.FlashcardCreateRequest;
import com.example.englishlearningplatform.dto.topic.FlashcardResponse;
import com.example.englishlearningplatform.dto.topic.FlashcardUpdateRequest;
import com.example.englishlearningplatform.service.FlashcardService;

@RestController
@RequestMapping("/api/admin")
public class AdminFlashcardController {

    private final FlashcardService flashcardService;

    public AdminFlashcardController(FlashcardService flashcardService) {
        this.flashcardService = flashcardService;
    }

    // POST lồng dưới topic — Flashcard phải gắn vào 1 Topic có thật khi tạo
    @PostMapping("/topics/{topicId}/flashcards")
    public ResponseEntity<FlashcardResponse> createFlashcard(
            @PathVariable Long topicId, @Valid @RequestBody FlashcardCreateRequest request) {
        return ResponseEntity.ok(flashcardService.createFlashcard(topicId, request));
    }

    // PUT/DELETE KHÔNG lồng topicId — đúng path đã chốt ở mục 5 Requirements
    @PutMapping("/flashcards/{id}")
    public ResponseEntity<FlashcardResponse> updateFlashcard(
            @PathVariable Long id, @Valid @RequestBody FlashcardUpdateRequest request) {
        return ResponseEntity.ok(flashcardService.updateFlashcard(id, request));
    }

    @DeleteMapping("/flashcards/{id}")
    public ResponseEntity<Void> deleteFlashcard(@PathVariable Long id) {
        flashcardService.deleteFlashcard(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/flashcards/{id}/image")
    public ResponseEntity<FlashcardResponse> uploadFlashcardImage(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(flashcardService.updateFlashcardImage(id, file));
    }
}