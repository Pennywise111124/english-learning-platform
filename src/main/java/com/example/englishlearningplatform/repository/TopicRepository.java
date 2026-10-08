package com.example.englishlearningplatform.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;

public interface TopicRepository extends JpaRepository<Topic, Long>, JpaSpecificationExecutor<Topic> {

    boolean existsByTitleAndLevel(String title, Level level);

    boolean existsByTitleAndLevelAndIdNot(String title, Level level, Long id);

    boolean existsByIdAndStatus(Long id, ContentStatus status);

    Optional<Topic> findByIdAndStatus(Long id, ContentStatus status);
}