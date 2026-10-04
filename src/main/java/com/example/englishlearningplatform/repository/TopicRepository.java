package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;

public interface TopicRepository extends JpaRepository<Topic, Long>, JpaSpecificationExecutor<Topic> {

    boolean existsByTitleAndLevel(String title, Level level);

    boolean existsByTitleAndLevelAndIdNot(String title, Level level, Long id);

}