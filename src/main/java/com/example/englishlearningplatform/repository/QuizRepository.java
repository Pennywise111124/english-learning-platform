package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Quiz;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByTopicId(Long topicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quiz q where q.id = :id")
    Optional<Quiz> findByIdForUpdate(@Param("id") Long id);

    boolean existsByTopicIdAndTitleIgnoreCase(Long topicId, String title);

    boolean existsByTopicIdAndTitleIgnoreCaseAndIdNot(Long topicId, String title, Long id);

    List<Quiz> findByTopic_IdAndStatus(Long topicId, ContentStatus status);

    Optional<Quiz> findByIdAndStatusAndTopic_Status(Long id, ContentStatus quizStatus, ContentStatus topicStatus);

    long countByTopic_IdAndStatus(Long topicId, ContentStatus status);
}