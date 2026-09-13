package com.example.englishlearningplatform.dto.quiz;

import com.example.englishlearningplatform.entity.QuizQuestion;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminQuizQuestionResponse {

    private final Long id;
    private final String question;
    private final List<String> options;
    private final String correctAnswer;

    public static AdminQuizQuestionResponse from(QuizQuestion question) {
        return new AdminQuizQuestionResponse(
                question.getId(),
                question.getQuestion(),
                question.getOptions(),
                question.getCorrectAnswer());
    }
}