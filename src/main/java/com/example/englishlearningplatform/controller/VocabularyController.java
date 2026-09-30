package com.example.englishlearningplatform.controller;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.dto.vocabulary.ReviewRequest;
import com.example.englishlearningplatform.dto.vocabulary.VocabularyResponse;
import com.example.englishlearningplatform.service.VocabularyService;
import com.example.englishlearningplatform.util.PaginationUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<VocabularyResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginationUtils.validate(page, size);
        return ResponseEntity.ok(vocabularyService.getAllSaved(page, size));
    }

    @GetMapping("/topics/{topicId}/saved-ids")
    public ResponseEntity<Set<Long>> getSavedFlashcardIds(@PathVariable Long topicId) {
        return ResponseEntity.ok(vocabularyService.getSavedFlashcardIds(topicId));
    }

    @PostMapping("/{flashcardId}/save")
    public ResponseEntity<VocabularyResponse> save(@PathVariable Long flashcardId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vocabularyService.saveWord(flashcardId));
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<VocabularyResponse> review(@PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(vocabularyService.review(id, request.getRemembered()));
    }

    @GetMapping("/today")
    public ResponseEntity<PageResponse<VocabularyResponse>> getToday(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginationUtils.validate(page, size);
        return ResponseEntity.ok(vocabularyService.getToday(page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vocabularyService.deleteSavedWord(id);
        return ResponseEntity.noContent().build();
    }
}
