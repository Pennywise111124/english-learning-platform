package com.example.englishlearningplatform.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.dto.dictation.DictationCatalogItem;
import com.example.englishlearningplatform.dto.dictation.DictationLessonResponse;
import com.example.englishlearningplatform.dto.dictation.DictationProgress;
import com.example.englishlearningplatform.dto.dictation.DictationResultResponse;
import com.example.englishlearningplatform.dto.dictation.DictationSort;
import com.example.englishlearningplatform.dto.dictation.DictationSubmitRequest;
import com.example.englishlearningplatform.dto.dictation.DictationSubmitResponse;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.service.DictationService;
import com.example.englishlearningplatform.util.PaginationUtils;

import jakarta.validation.Valid;

@RestController
public class DictationController {
    private static final int MAX_KEYWORD_LENGTH = 50;

    private final DictationService dictationService;

    public DictationController(DictationService dictationService) {
        this.dictationService = dictationService;
    }

    @GetMapping("/api/dictation/lessons")
    public ResponseEntity<PageResponse<DictationCatalogItem>> getCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String progress,
            @RequestParam(required = false) String sort) {

        PaginationUtils.validate(page, size);

        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("Keyword must not exceed " + MAX_KEYWORD_LENGTH + " characters");
        }

        Level levelFilter = null;
        if (level != null && !level.isBlank()) {
            try {
                levelFilter = Level.valueOf(level.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Invalid level value. Valid values are: beginner, intermediate, advanced");
            }
        }

        DictationSort dictationSort = DictationSort.from(sort);
        DictationProgress dictationProgress = DictationProgress.from(progress);

        return ResponseEntity.ok(dictationService.getCatalog(
                normalizedKeyword, topicId, levelFilter, dictationProgress, dictationSort, page, size));
    }

    @GetMapping("/api/topics/{topicId}/dictation")
    public ResponseEntity<List<DictationLessonResponse>> getLessons(@PathVariable Long topicId) {
        return ResponseEntity.ok(dictationService.getLessonsByTopic(topicId));
    }

    @PostMapping("/api/dictation/{id}/submit")
    public ResponseEntity<DictationSubmitResponse> submit(@PathVariable Long id,
            @Valid @RequestBody DictationSubmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dictationService.submit(id, request));
    }

    @GetMapping("/api/dictation/{id}/results")
    public ResponseEntity<PageResponse<DictationResultResponse>> getResults(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        PaginationUtils.validate(page, size);
        return ResponseEntity.ok(dictationService.getMyResults(id, page, size));
    }
}
