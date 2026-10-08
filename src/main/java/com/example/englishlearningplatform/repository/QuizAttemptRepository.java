package com.example.englishlearningplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.QuizAttempt;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

        Page<QuizAttempt> findByUser_IdAndQuiz_Id(Long userId, Long quizId, Pageable pageable);

        Page<QuizAttempt> findByUser_IdOrderByCompletedAtDesc(Long userId, Pageable pageable);

        boolean existsByQuiz_Id(Long quizId);

        boolean existsByQuiz_Topic_Id(Long topicId);

        @Query("SELECT COUNT(DISTINCT qa.quiz.id) FROM QuizAttempt qa "
                        + "WHERE qa.user.id = :userId "
                        + "AND qa.quiz.topic.id = :topicId "
                        + "AND qa.score >= :minScore "
                        + "AND qa.quiz.status = :status")
        long countAchievedQuizzesInTopic(@Param("userId") Long userId,
                        @Param("topicId") Long topicId,
                        @Param("minScore") int minScore,
                        @Param("status") ContentStatus status);
}