package com.example.englishlearningplatform.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.dto.quiz.QuizSummaryResponse;
import com.example.englishlearningplatform.dto.topic.FlashcardResponse;
import com.example.englishlearningplatform.dto.topic.TopicResponse;
import com.example.englishlearningplatform.dto.topic.TopicSort;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.service.FlashcardService;
import com.example.englishlearningplatform.service.QuizService;
import com.example.englishlearningplatform.service.TopicService;
import com.example.englishlearningplatform.util.PaginationUtils;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;
    private final QuizService quizService;
    private final FlashcardService flashcardService;

    private static final int MAX_KEYWORD_LENGTH = 50;

    public TopicController(TopicService topicService, QuizService quizService, FlashcardService flashcardService) {
        this.topicService = topicService;
        this.quizService = quizService;
        this.flashcardService = flashcardService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<TopicResponse>> getTopics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
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

        TopicSort topicSort = TopicSort.from(sort);

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(topicService.getTopics(normalizedKeyword, levelFilter, topicSort, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TopicResponse> getTopicById(@PathVariable Long id) {
        return ResponseEntity.ok(topicService.getTopicById(id));
    }

    @GetMapping("/{id}/quizzes")
    public ResponseEntity<List<QuizSummaryResponse>> getQuizzesByTopic(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizzesByTopic(id));
    }

    @GetMapping("/{id}/flashcards")
    public ResponseEntity<List<FlashcardResponse>> getFlashcards(@PathVariable Long id) {
        return ResponseEntity.ok(flashcardService.getFlashcardsByTopic(id));
    }
}