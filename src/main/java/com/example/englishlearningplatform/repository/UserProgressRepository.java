package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.englishlearningplatform.entity.UserProgress;

import java.util.List;
import java.util.Optional;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    Optional<UserProgress> findByUser_IdAndTopic_Id(Long userId, Long topicId);

    @EntityGraph(attributePaths = "topic")
    List<UserProgress> findByUser_Id(Long userId);

    boolean existsByTopic_Id(Long topicId);
}