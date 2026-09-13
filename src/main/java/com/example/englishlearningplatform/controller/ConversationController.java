package com.example.englishlearningplatform.controller;

import com.example.englishlearningplatform.dto.chat.*;
import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.service.ChatService;
import com.example.englishlearningplatform.util.PaginationUtils;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChatService chatService;

    public ConversationController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ConversationDetailResponse> createConversation(Authentication authentication) {
        ConversationDetailResponse response = chatService.createConversation(authentication.getName());
        return ResponseEntity.created(URI.create("/api/conversations/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ConversationSummaryResponse>> getConversations(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginationUtils.validate(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<ConversationSummaryResponse> pageResult = chatService.getConversations(authentication.getName(), pageable);
        return ResponseEntity.ok(PageResponse.from(pageResult));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDetailResponse> getConversation(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(chatService.getConversation(authentication.getName(), id));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessages(authentication.getName(), id));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(authentication.getName(), id, request));
    }
}