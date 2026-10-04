package com.example.englishlearningplatform.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.englishlearningplatform.entity.DictationLesson;

public interface DictationLessonRepository
        extends JpaRepository<DictationLesson, Long>, JpaSpecificationExecutor<DictationLesson> {
    List<DictationLesson> findByTopic_IdOrderByIdAsc(Long topicId); // Admin

    List<DictationLesson> findByTopic_IdAndMediaUrlIsNotNullOrderByIdAsc(Long topicId); // User

    @Query("SELECT l.mediaUrl FROM DictationLesson l WHERE l.topic.id = :topicId AND l.mediaUrl IS NOT NULL")
    List<String> findMediaUrlsByTopicId(@Param("topicId") Long topicId);

    @Override
    @EntityGraph(attributePaths = "topic")
    Page<DictationLesson> findAll(Specification<DictationLesson> spec, Pageable pageable);
}
