package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.englishlearningplatform.entity.Quiz;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByTopicId(Long topicId);

    long countByTopicId(Long topicId);

    boolean existsByTopicIdAndTitleIgnoreCase(Long topicId, String title);

    boolean existsByTopicIdAndTitleIgnoreCaseAndIdNot(Long topicId, String title, Long id);
}