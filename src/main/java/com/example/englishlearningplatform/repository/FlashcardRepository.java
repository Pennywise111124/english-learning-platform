package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Flashcard;

import java.util.List;
import java.util.Optional;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findByTopicId(Long topicId);

    Optional<Flashcard> findByIdAndTopic_Status(Long id, ContentStatus topicStatus);
}