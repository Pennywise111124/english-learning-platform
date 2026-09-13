package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.englishlearningplatform.entity.Flashcard;

import java.util.List;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    // Theo mục 5: GET /api/topics/{id}/flashcards KHÔNG phân trang
    // (danh sách nhỏ, cố định theo 1 topic) — trả thẳng List, không cần Page
    List<Flashcard> findByTopicId(Long topicId);
}