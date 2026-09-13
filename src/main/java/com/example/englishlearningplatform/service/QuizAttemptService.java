package com.example.englishlearningplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.englishlearningplatform.dto.quiz.*;
import com.example.englishlearningplatform.entity.*;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizAttemptService {

    private static final int PASS_SCORE_THRESHOLD = 70;

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;

    public QuizAttemptService(QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizAttemptRepository quizAttemptRepository,
            UserProgressRepository userProgressRepository,
            UserRepository userRepository) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.userProgressRepository = userProgressRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public QuizResultResponse submitQuiz(Long quizId, SubmitQuizRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại!"));

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại!"));

        List<QuizQuestion> realQuestions = quizQuestionRepository.findByQuizId(quizId);

        Map<Long, QuizQuestion> questionMap = realQuestions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        int correctCount = 0;
        Set<Long> processedQuestionIds = new HashSet<>();

        if (request.getAnswers() != null) {
            for (SubmitAnswerItem item : request.getAnswers()) {
                Long qId = item.getQuestionId();
                String userAnswers = item.getAnswer();

                QuizQuestion question = questionMap.get(qId);
                if (question == null) {
                    throw new IllegalArgumentException("Câu hỏi ID " + qId + " không thuộc Quiz này!");
                }

                if (!processedQuestionIds.add(qId)) {
                    throw new IllegalArgumentException("Phát hiện câu hỏi trùng lặp ID: " + qId);
                }

                if (userAnswers == null || userAnswers.isBlank() || !question.getOptions().contains(userAnswers)) {
                    throw new IllegalArgumentException(
                            "Đáp án không hợp lệ hoặc không nằm trong tập lựa chọn của câu hỏi ID: " + qId);
                }

                if (userAnswers.trim().equalsIgnoreCase(question.getCorrectAnswer().trim())) {
                    correctCount++;
                }
            }
        }

        int totalQuestions = realQuestions.size();
        int score = 0;
        if (totalQuestions > 0) {
            score = (int) Math.round(100.0 * correctCount / totalQuestions);
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(user);
        attempt.setQuiz(quiz);
        attempt.setScore(score);
        attempt.setCorrectAnswers(correctCount);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setCompletedAt(Instant.now());

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        updateProgressAfterAttempt(user, quiz);

        return new QuizResultResponse(savedAttempt.getScore(),
                savedAttempt.getCorrectAnswers(),
                savedAttempt.getTotalQuestions(),
                savedAttempt.getCompletedAt());
    }

    private void updateProgressAfterAttempt(User user, Quiz quiz) {
        Topic topic = quiz.getTopic();

        long achievedCount = quizAttemptRepository.countAchievedQuizzesInTopic(
                user.getId(), topic.getId(), PASS_SCORE_THRESHOLD);

        long totalQuizCount = quizRepository.countByTopicId(topic.getId());

        int percent = 0;
        if (totalQuizCount > 0) {
            percent = (int) Math.round(100.0 * achievedCount / totalQuizCount);
        }

        ProgressStatus status = (totalQuizCount > 0 && achievedCount == totalQuizCount)
                ? ProgressStatus.COMPLETED
                : ProgressStatus.IN_PROGRESS;

        UserProgress userProgress = userProgressRepository
                .findByUser_IdAndTopic_Id(user.getId(), topic.getId())
                .orElseGet(() -> {
                    UserProgress newProgress = new UserProgress();
                    newProgress.setUser(user);
                    newProgress.setTopic(topic);
                    return newProgress;
                });

        userProgress.setProgressPercent(percent);
        userProgress.setStatus(status);
        userProgress.setUpdatedAt(Instant.now());

        userProgressRepository.save(userProgress);
    }

    @Transactional(readOnly = true)
    public Page<QuizAttemptResponse> getMyAttempts(Long quizId, Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Username " + username + " không tồn tại!"));

        return quizAttemptRepository.findByUser_IdAndQuiz_Id(user.getId(), quizId, pageable)
                .map(QuizAttemptResponse::from);
    }
}