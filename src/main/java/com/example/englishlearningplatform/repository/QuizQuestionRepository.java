package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.englishlearningplatform.entity.QuizQuestion;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuizId(Long quizId);
}