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
import com.example.englishlearningplatform.util.TopicQueryParser;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;
    private final QuizService quizService;
    private final FlashcardService flashcardService;

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

        String normalizedKeyword = TopicQueryParser.keyword(keyword);
        Level levelFilter = TopicQueryParser.level(level);

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