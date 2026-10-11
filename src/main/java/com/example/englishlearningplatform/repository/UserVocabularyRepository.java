package com.example.englishlearningplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.englishlearningplatform.entity.UserVocabulary;
import com.example.englishlearningplatform.entity.VocabularyStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, Long> {

    boolean existsByUser_IdAndFlashcard_Id(Long userId, Long flashcardId);

    Optional<UserVocabulary> findByIdAndUser_Id(Long id, Long userId);

    @EntityGraph(attributePaths = "flashcard")
    Page<UserVocabulary> findByUser_IdAndNextReviewAtLessThanEqual(Long userId, Instant now, Pageable pageable);

    @Query("SELECT uv.flashcard.id FROM UserVocabulary uv WHERE uv.user.id = :userId AND uv.flashcard.topic.id = :topicId")
    List<Long> findFlashcardIdsByUserIdAndTopicId(@Param("userId") Long userId, @Param("topicId") Long topicId);

    @EntityGraph(attributePaths = "flashcard")
    Page<UserVocabulary> findByUser_Id(Long userId, Pageable pageable);

    boolean existsByFlashcard_Id(Long flashcardId);

    boolean existsByFlashcard_Topic_Id(Long topicId);

    long countByUser_IdAndStatus(Long userId, VocabularyStatus status);
}