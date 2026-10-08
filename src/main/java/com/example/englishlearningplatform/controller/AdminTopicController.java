package com.example.englishlearningplatform.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.dto.common.StatusChangeRequest;
import com.example.englishlearningplatform.dto.topic.TopicCreateRequest;
import com.example.englishlearningplatform.dto.topic.TopicResponse;
import com.example.englishlearningplatform.dto.topic.TopicSort;
import com.example.englishlearningplatform.dto.topic.TopicUpdateRequest;
import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.service.TopicService;
import com.example.englishlearningplatform.util.PaginationUtils;
import com.example.englishlearningplatform.util.TopicQueryParser;

@RestController
@RequestMapping("/api/admin/topics")
public class AdminTopicController {

    private final TopicService topicService;

    public AdminTopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<TopicResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String status) {

        PaginationUtils.validate(page, size);

        String normalizedKeyword = TopicQueryParser.keyword(keyword);
        Level levelFilter = TopicQueryParser.level(level);
        ContentStatus statusFilter = TopicQueryParser.status(status);
        TopicSort topicSort = TopicSort.from(sort);

        return ResponseEntity.ok(topicService.getTopicsForAdmin(
                normalizedKeyword, levelFilter, topicSort, statusFilter, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TopicResponse> getTopic(@PathVariable Long id) {
        return ResponseEntity.ok(topicService.getTopicForAdmin(id));
    }

    @PostMapping
    public ResponseEntity<TopicResponse> createTopic(@Valid @RequestBody TopicCreateRequest request) {
        return ResponseEntity.ok(topicService.createTopic(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TopicResponse> updateTopic(@PathVariable Long id,
            @Valid @RequestBody TopicUpdateRequest request) {
        return ResponseEntity.ok(topicService.updateTopic(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        topicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<TopicResponse> uploadTopicImage(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(topicService.updateTopicImage(id, file));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TopicResponse> changeStatus(@PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request) {
        return ResponseEntity.ok(topicService.changeStatus(id, request.status()));
    }
}