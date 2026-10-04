package com.example.englishlearningplatform.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.englishlearningplatform.entity.DictationResult;

public interface DictationResultRepository extends JpaRepository<DictationResult, Long> {
    Page<DictationResult> findByUser_IdAndLesson_Id(Long userId, Long lessonId, Pageable pageable);

    @Query("""
            SELECT r.lesson.id AS lessonId, COUNT(r) AS attempts,
                   MAX(r.accuracy) AS bestAccuracy, MAX(r.createdAt) AS lastAttemptAt
            FROM DictationResult r
            WHERE r.user.id = :userId AND r.lesson.id IN :lessonIds
            GROUP BY r.lesson.id
            """)
    List<DictationLessonStats> findStatsByUserIdAndLessonIds(
            @Param("userId") Long userId, @Param("lessonIds") Collection<Long> lessonIds);

    boolean existsByLesson_Id(Long lessonId);

    boolean existsByLesson_Topic_Id(Long topicId);
}
