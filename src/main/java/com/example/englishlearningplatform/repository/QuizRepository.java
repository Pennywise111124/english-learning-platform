package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.englishlearningplatform.entity.Quiz;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByTopicId(Long topicId);

    // Dùng làm mẫu số trong công thức progressPercent (tổng số Quiz thuộc Topic)
    long countByTopicId(Long topicId);
}