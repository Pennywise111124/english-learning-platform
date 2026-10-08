package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Quiz;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByTopicId(Long topicId);

    boolean existsByTopicIdAndTitleIgnoreCase(Long topicId, String title);

    boolean existsByTopicIdAndTitleIgnoreCaseAndIdNot(Long topicId, String title, Long id);

    List<Quiz> findByTopic_IdAndStatus(Long topicId, ContentStatus status);

    Optional<Quiz> findByIdAndStatusAndTopic_Status(Long id, ContentStatus quizStatus, ContentStatus topicStatus);

    long countByTopic_IdAndStatus(Long topicId, ContentStatus status);
}