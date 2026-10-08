package com.example.englishlearningplatform.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.englishlearningplatform.dto.common.StatusChangeRequest;
import com.example.englishlearningplatform.dto.dictation.AdminDictationLessonResponse;
import com.example.englishlearningplatform.dto.dictation.DictationLessonRequest;
import com.example.englishlearningplatform.service.DictationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminDictationController {
    private final DictationService dictationService;

    public AdminDictationController(DictationService dictationService) {
        this.dictationService = dictationService;
    }

    @GetMapping("/topics/{topicId}/dictation")
    public ResponseEntity<List<AdminDictationLessonResponse>> getLessons(@PathVariable Long topicId) {
        return ResponseEntity.ok(dictationService.getLessonsForAdmin(topicId));
    }

    @PostMapping("/topics/{topicId}/dictation")
    public ResponseEntity<AdminDictationLessonResponse> create(@PathVariable Long topicId,
            @Valid @RequestBody DictationLessonRequest request) {
        return ResponseEntity.ok(dictationService.createLesson(topicId, request));
    }

    @PutMapping("/dictation/{id}")
    public ResponseEntity<AdminDictationLessonResponse> update(@PathVariable Long id,
            @Valid @RequestBody DictationLessonRequest request) {
        return ResponseEntity.ok(dictationService.updateLesson(id, request));
    }

    @DeleteMapping("/dictation/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dictationService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/dictation/{id}/audio")
    public ResponseEntity<AdminDictationLessonResponse> uploadAudio(@PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(dictationService.uploadAudio(id, file));
    }

    @PatchMapping("/dictation/{id}/status")
    public ResponseEntity<AdminDictationLessonResponse> changeStatus(@PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request) {
        return ResponseEntity.ok(dictationService.changeStatus(id, request.status()));
    }
}