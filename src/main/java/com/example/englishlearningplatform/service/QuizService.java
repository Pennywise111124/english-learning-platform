package com.example.englishlearningplatform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.englishlearningplatform.dto.quiz.*;
import com.example.englishlearningplatform.entity.Quiz;
import com.example.englishlearningplatform.entity.QuizQuestion;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.exception.ResourceConflictException;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.QuizAttemptRepository;
import com.example.englishlearningplatform.repository.QuizQuestionRepository;
import com.example.englishlearningplatform.repository.QuizRepository;
import com.example.englishlearningplatform.repository.TopicRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final TopicRepository topicRepository;

    public QuizService(QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizAttemptRepository quizAttemptRepository,
            TopicRepository topicRepository) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.topicRepository = topicRepository;
    }

    // ══════════════════ USER-FACING ══════════════════

    @Transactional(readOnly = true)
    public List<QuizSummaryResponse> getQuizzesByTopic(Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic not found with id: " + topicId);
        }
        return quizRepository.findByTopicId(topicId).stream()
                .map(quiz -> QuizSummaryResponse.from(quiz, quizQuestionRepository.countByQuizId(quiz.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizDetail(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        List<QuizQuestionPublicResponse> questions = quizQuestionRepository.findByQuizId(quizId).stream()
                .map(QuizQuestionPublicResponse::from)
                .collect(Collectors.toList());

        return QuizDetailResponse.of(quiz, questions);
    }

    // ══════════════════ ADMIN-FACING ══════════════════

    @Transactional(readOnly = true)
    public List<AdminQuizQuestionResponse> getQuestionsForAdmin(Long quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz not found with id: " + quizId);
        }
        return quizQuestionRepository.findByQuizId(quizId).stream()
                .map(AdminQuizQuestionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public QuizSummaryResponse createQuiz(Long topicId, QuizRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        if (quizRepository.existsByTopicIdAndTitleIgnoreCase(topicId, request.getTitle())) {
            throw new ResourceConflictException("This topic already has a quiz with the title: " + request.getTitle());
        }

        Quiz quiz = new Quiz();
        quiz.setTopic(topic);
        quiz.setTitle(request.getTitle());

        return QuizSummaryResponse.from(quizRepository.save(quiz), 0);
    }

    @Transactional
    public QuizSummaryResponse updateQuiz(Long quizId, QuizRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        if (quizRepository.existsByTopicIdAndTitleIgnoreCaseAndIdNot(quiz.getTopic().getId(), request.getTitle(),
                quizId)) {
            throw new ResourceConflictException("This topic already has a quiz with the title: " + request.getTitle());
        }

        quiz.setTitle(request.getTitle());
        Quiz updated = quizRepository.save(quiz);
        return QuizSummaryResponse.from(updated, quizQuestionRepository.countByQuizId(updated.getId()));
    }

    @Transactional
    public void deleteQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        if (quizAttemptRepository.existsByQuiz_Id(quizId)) {
            throw new ResourceConflictException("Quiz already has attempts from users");
        }

        quizRepository.delete(quiz);
    }

    @Transactional
    public QuizQuestionPublicResponse createQuestion(Long quizId, QuizQuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        QuizQuestion question = new QuizQuestion();
        question.setQuiz(quiz);
        question.setQuestion(request.getQuestion());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());

        return QuizQuestionPublicResponse.from(quizQuestionRepository.save(question));
    }

    @Transactional
    public QuizQuestionPublicResponse updateQuestion(Long questionId, QuizQuestionRequest request) {
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        question.setQuestion(request.getQuestion());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());

        return QuizQuestionPublicResponse.from(quizQuestionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        quizQuestionRepository.delete(question);
    }
}