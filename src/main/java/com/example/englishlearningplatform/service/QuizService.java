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
            throw new ResourceNotFoundException("Topic không tồn tại với Id: " + topicId);
        }
        return quizRepository.findByTopicId(topicId).stream()
                .map(QuizSummaryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizDetail(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại với Id: " + quizId));

        List<QuizQuestionPublicResponse> questions = quizQuestionRepository.findByQuizId(quizId).stream()
                .map(QuizQuestionPublicResponse::from)
                .collect(Collectors.toList());

        return QuizDetailResponse.of(quiz, questions);
    }

    // ══════════════════ ADMIN-FACING ══════════════════

    @Transactional(readOnly = true)
    public List<AdminQuizQuestionResponse> getQuestionsForAdmin(Long quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz không tồn tại với Id: " + quizId);
        }
        return quizQuestionRepository.findByQuizId(quizId).stream()
                .map(AdminQuizQuestionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public QuizSummaryResponse createQuiz(Long topicId, QuizRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic không tồn tại với Id: " + topicId));

        Quiz quiz = new Quiz();
        quiz.setTopic(topic);
        quiz.setTitle(request.getTitle());

        return QuizSummaryResponse.from(quizRepository.save(quiz));
    }

    @Transactional
    public QuizSummaryResponse updateQuiz(Long quizId, QuizRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại với Id: " + quizId));

        quiz.setTitle(request.getTitle());
        return QuizSummaryResponse.from(quizRepository.save(quiz));
    }

    /**
     * DELETE /api/admin/quizzes/{id}
     *
     * TODO: check quizAttemptRepository.existsByQuiz_Id(quizId) TRƯỚC khi xoá —
     * nếu true, throw ResourceConflictException (409). Đây là bài tập lặp lại
     * CÙNG PATTERN vừa làm ở TopicService.deleteTopic() phía trên, chỉ khác
     * repository/field — cố ý để bạn tự áp dụng lại không cần nhắc chi tiết.
     * Nếu pass check, xoá bình thường — DB tự cascade QuizQuestion (+ options
     * qua ElementCollection) theo FK đã cấu hình.
     */
    @Transactional
    public void deleteQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại với Id: " + quizId));

        if (quizAttemptRepository.existsByQuiz_Id(quizId)) {
            throw new ResourceConflictException("Quiz đã có bài làm từ người dùng.");
        }

        quizRepository.delete(quiz);
    }

    @Transactional
    public QuizQuestionPublicResponse createQuestion(Long quizId, QuizQuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại với Id: " + quizId));

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
                .orElseThrow(() -> new ResourceNotFoundException("Câu hỏi không tồn tại với Id: " + questionId));

        question.setQuestion(request.getQuestion());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());

        return QuizQuestionPublicResponse.from(quizQuestionRepository.save(question));
    }

    // Không cần check 409 khi xoá Question — QuizAttempt chỉ lưu kết quả TỔNG
    // QUAN (score/correctAnswers/totalQuestions), không tham chiếu tới từng
    // Question cụ thể nào (đã chốt phạm vi ở mục 4 Requirements), nên xoá 1
    // Question không phá vỡ tính toàn vẹn của QuizAttempt cũ.
    @Transactional
    public void deleteQuestion(Long questionId) {
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Câu hỏi không tồn tại với Id: " + questionId));

        quizQuestionRepository.delete(question);
    }
}