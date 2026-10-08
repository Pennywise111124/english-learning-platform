package com.example.englishlearningplatform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.englishlearningplatform.dto.quiz.QuizRequest;
import com.example.englishlearningplatform.dto.quiz.QuizSummaryResponse;
import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Quiz;
import com.example.englishlearningplatform.entity.QuizQuestion;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.exception.ResourceConflictException;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.QuizQuestionRepository;
import com.example.englishlearningplatform.repository.QuizRepository;
import com.example.englishlearningplatform.repository.TopicRepository;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    private static final Long TOPIC_ID = 10L;
    private static final Long QUIZ_ID = 100L;

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @InjectMocks
    private QuizService quizService;

    private Quiz testQuiz;

    @BeforeEach
    void setUp() {
        Topic topic = new Topic();
        topic.setId(TOPIC_ID);
        topic.setTitle("Grammar Basics");

        testQuiz = new Quiz();
        testQuiz.setId(QUIZ_ID);
        testQuiz.setTopic(topic);
        testQuiz.setTitle("Present Simple Quiz");
    }

    // ------------------------------------------------------------------
    // getQuizzesByTopic()
    // ------------------------------------------------------------------

    @Test
    void getQuizzesByTopic_whenTopicNotPublished_throwsNotFound() {
        when(topicRepository.existsByIdAndStatus(TOPIC_ID, ContentStatus.PUBLISHED)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> quizService.getQuizzesByTopic(TOPIC_ID));

        verify(topicRepository, never()).existsById(any());
        verifyNoInteractions(quizRepository);
    }

    @Test
    void getQuizzesByTopic_whenPublished_usesOnlyStatusFilteredQueries() {
        when(topicRepository.existsByIdAndStatus(TOPIC_ID, ContentStatus.PUBLISHED)).thenReturn(true);
        when(quizRepository.findByTopic_IdAndStatus(TOPIC_ID, ContentStatus.PUBLISHED))
                .thenReturn(List.of(testQuiz));

        var result = quizService.getQuizzesByTopic(TOPIC_ID);

        assertEquals(1, result.size());
        verify(topicRepository).existsByIdAndStatus(TOPIC_ID, ContentStatus.PUBLISHED);
        verify(quizRepository).findByTopic_IdAndStatus(TOPIC_ID, ContentStatus.PUBLISHED);
        verifyNoMoreInteractions(topicRepository, quizRepository);
    }

    // ------------------------------------------------------------------
    // getQuizDetail()
    // ------------------------------------------------------------------

    @Test
    void getQuizDetail_whenQuizNotPublished_throwsNotFound() {
        when(quizRepository.findByIdAndStatusAndTopic_Status(QUIZ_ID, ContentStatus.PUBLISHED,
                ContentStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> quizService.getQuizDetail(QUIZ_ID));

        verify(quizRepository, never()).findById(any());
        verifyNoInteractions(quizQuestionRepository);
    }

    @Test
    void getQuizDetail_whenPublished_usesOnlyStatusFilteredQuery() {
        when(quizRepository.findByIdAndStatusAndTopic_Status(QUIZ_ID, ContentStatus.PUBLISHED,
                ContentStatus.PUBLISHED)).thenReturn(Optional.of(testQuiz));
        lenient().when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of());

        var result = quizService.getQuizDetail(QUIZ_ID);

        assertNotNull(result);
        verify(quizRepository).findByIdAndStatusAndTopic_Status(QUIZ_ID, ContentStatus.PUBLISHED,
                ContentStatus.PUBLISHED);
        verifyNoMoreInteractions(quizRepository);
    }

    // ------------------------------------------------------------------
    // changeStatus()
    // ------------------------------------------------------------------

    @Test
    void changeStatus_whenNotFound_throws404() {
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> quizService.changeStatus(QUIZ_ID, ContentStatus.PUBLISHED));

        verify(quizRepository, never()).save(any());
    }

    @Test
    void changeStatus_publishQuizWithNoQuestions_throwsConflict() {
        testQuiz.setStatus(ContentStatus.DRAFT);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(testQuiz));
        // ASSUMPTION: tên query đếm câu hỏi (QuizSummaryResponse có questionCount nên
        // nhiều khả năng đã có)
        when(quizQuestionRepository.countByQuizId(QUIZ_ID)).thenReturn(0L);

        assertThrows(ResourceConflictException.class,
                () -> quizService.changeStatus(QUIZ_ID, ContentStatus.PUBLISHED));

        verify(quizRepository, never()).save(any());
        assertEquals(ContentStatus.DRAFT, testQuiz.getStatus()); // entity không bị đổi trước khi kiểm tra
    }

    @Test
    void changeStatus_publishQuizWithQuestions_succeeds() {
        testQuiz.setStatus(ContentStatus.DRAFT);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(testQuiz));
        when(quizQuestionRepository.countByQuizId(QUIZ_ID)).thenReturn(3L);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        quizService.changeStatus(QUIZ_ID, ContentStatus.PUBLISHED);

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(captor.capture());
        assertEquals(ContentStatus.PUBLISHED, captor.getValue().getStatus());
        assertEquals("Present Simple Quiz", captor.getValue().getTitle()); // chỉ đổi status
    }

    @Test
    void changeStatus_archiveQuizWithNoQuestions_succeeds() {
        testQuiz.setStatus(ContentStatus.PUBLISHED);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(testQuiz));
        // lenient: kịch bản "quiz rỗng" được nêu rõ, nhưng không bắt service phải gọi
        // đếm khi không publish
        lenient().when(quizQuestionRepository.countByQuizId(QUIZ_ID)).thenReturn(0L);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        quizService.changeStatus(QUIZ_ID, ContentStatus.ARCHIVED);

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(captor.capture());
        assertEquals(ContentStatus.ARCHIVED, captor.getValue().getStatus());
    }

    // ------------------------------------------------------------------
    // createQuiz()
    // ------------------------------------------------------------------

    @Test
    void createQuiz_newQuizIsDraft() {
        // ASSUMPTION: chữ ký createQuiz(topicId, request) và DTO. Sửa theo QuizService
        // thật
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testQuiz.getTopic()));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizRequest request = new QuizRequest();
        request.setTitle("New Quiz");

        quizService.createQuiz(TOPIC_ID, request);

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(captor.capture());
        assertEquals(ContentStatus.DRAFT, captor.getValue().getStatus());
    }

    // ------------------------------------------------------------------
    // getQuizzesForAdmin()
    // ------------------------------------------------------------------

    private Quiz quizWith(Long id, String title, ContentStatus status) {
        Quiz q = new Quiz();
        q.setId(id);
        q.setTitle(title);
        q.setTopic(testQuiz.getTopic());
        q.setStatus(status);
        return q;
    }

    @Test
    void getQuizzesForAdmin_whenTopicNotFound_throws404() {
        when(topicRepository.existsById(TOPIC_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> quizService.getQuizzesForAdmin(TOPIC_ID));

        verify(topicRepository, never()).existsByIdAndStatus(any(), any());
        verifyNoInteractions(quizRepository);
    }

    @Test
    void getQuizzesForAdmin_returnsQuizzesOfEveryStatusWithQuestionCounts() {
        when(topicRepository.existsById(TOPIC_ID)).thenReturn(true);
        when(quizRepository.findByTopicId(TOPIC_ID)).thenReturn(List.of(
                quizWith(1L, "Draft", ContentStatus.DRAFT),
                quizWith(2L, "Published", ContentStatus.PUBLISHED),
                quizWith(3L, "Archived", ContentStatus.ARCHIVED)));
        when(quizQuestionRepository.countByQuizId(1L)).thenReturn(0L);
        when(quizQuestionRepository.countByQuizId(2L)).thenReturn(4L);
        when(quizQuestionRepository.countByQuizId(3L)).thenReturn(2L);

        var result = quizService.getQuizzesForAdmin(TOPIC_ID);

        assertEquals(List.of(ContentStatus.DRAFT, ContentStatus.PUBLISHED, ContentStatus.ARCHIVED),
                result.stream().map(QuizSummaryResponse::getStatus).toList());
        assertEquals(List.of(0L, 4L, 2L),
                result.stream().map(QuizSummaryResponse::getQuestionCount).toList());
        verify(quizRepository, never()).findByTopic_IdAndStatus(any(), any());
    }

    // ------------------------------------------------------------------
    // deleteQuestion()
    // ------------------------------------------------------------------

    private QuizQuestion questionOf(Quiz quiz) {
        QuizQuestion q = new QuizQuestion();
        q.setId(500L);
        q.setQuiz(quiz);
        return q;
    }

    @Test
    void deleteQuestion_whenNotFound_throws404() {
        when(quizQuestionRepository.findById(500L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> quizService.deleteQuestion(500L));

        verify(quizQuestionRepository, never()).delete(any());
    }

    @Test
    void deleteQuestion_lastQuestionOfPublishedQuiz_throwsConflict() {
        testQuiz.setStatus(ContentStatus.PUBLISHED);
        QuizQuestion question = questionOf(testQuiz);
        when(quizQuestionRepository.findById(500L)).thenReturn(Optional.of(question));
        when(quizQuestionRepository.countByQuizId(QUIZ_ID)).thenReturn(1L);

        assertThrows(ResourceConflictException.class, () -> quizService.deleteQuestion(500L));

        verify(quizQuestionRepository, never()).delete(any());
    }

    @Test
    void deleteQuestion_notTheLastQuestionOfPublishedQuiz_succeeds() {
        testQuiz.setStatus(ContentStatus.PUBLISHED);
        QuizQuestion question = questionOf(testQuiz);
        when(quizQuestionRepository.findById(500L)).thenReturn(Optional.of(question));
        when(quizQuestionRepository.countByQuizId(QUIZ_ID)).thenReturn(2L);

        quizService.deleteQuestion(500L);

        verify(quizQuestionRepository).delete(question);
    }

    @ParameterizedTest
    @EnumSource(value = ContentStatus.class, names = { "DRAFT", "ARCHIVED" })
    void deleteQuestion_lastQuestionOfNonPublishedQuiz_succeeds(ContentStatus status) {
        testQuiz.setStatus(status);
        QuizQuestion question = questionOf(testQuiz);
        when(quizQuestionRepository.findById(500L)).thenReturn(Optional.of(question));
        lenient().when(quizQuestionRepository.countByQuizId(QUIZ_ID)).thenReturn(1L);

        quizService.deleteQuestion(500L);

        verify(quizQuestionRepository).delete(question);
    }
}