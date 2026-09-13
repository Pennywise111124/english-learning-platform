package com.example.englishlearningplatform.service;

import com.example.englishlearningplatform.dto.quiz.SubmitAnswerItem;
import com.example.englishlearningplatform.dto.quiz.SubmitQuizRequest;
import com.example.englishlearningplatform.dto.quiz.QuizAttemptResponse;
import com.example.englishlearningplatform.dto.quiz.QuizResultResponse;
import com.example.englishlearningplatform.entity.*;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private UserProgressRepository userProgressRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuizAttemptService quizAttemptService;

    private User testUser;
    private Topic testTopic;
    private Quiz testQuiz;
    private QuizQuestion question1;
    private QuizQuestion question2;

    private static final String TEST_USERNAME = "testuser";
    private static final Long QUIZ_ID = 100L;
    private static final int PASSING_SCORE = 70;

    @BeforeEach
    void setUp() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USERNAME, null);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(quizAttemptRepository.save(any(QuizAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);

        testTopic = new Topic();
        testTopic.setId(10L);
        testTopic.setTitle("Grammar Basics");

        testQuiz = new Quiz();
        testQuiz.setId(QUIZ_ID);
        testQuiz.setTitle("Present Simple Quiz");
        testQuiz.setTopic(testTopic);

        question1 = new QuizQuestion();
        question1.setId(1001L);
        question1.setQuiz(testQuiz);
        question1.setCorrectAnswer("Hanoi");
        question1.setOptions(List.of("Hanoi", "Saigon", "Danang"));

        question2 = new QuizQuestion();
        question2.setId(1002L);
        question2.setQuiz(testQuiz);
        question2.setCorrectAnswer("Cat");
        question2.setOptions(List.of("Cat", "Dog", "Bird"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SubmitAnswerItem createAnswerItem(Long questionId, String answer) {
        SubmitAnswerItem item = new SubmitAnswerItem();
        item.setQuestionId(questionId);
        item.setAnswer(answer);
        return item;
    }

    private void mockSuccessfulUserAndQuiz() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(testQuiz));
    }

    // ------------------------------------------------------------------
    // submitQuiz()
    // ------------------------------------------------------------------

    @Test
    void submitQuiz_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        SubmitQuizRequest request = new SubmitQuizRequest();

        assertThrows(ResourceNotFoundException.class,
                () -> quizAttemptService.submitQuiz(QUIZ_ID, request));
    }

    @Test
    void submitQuiz_whenQuizNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

        SubmitQuizRequest request = new SubmitQuizRequest();

        assertThrows(ResourceNotFoundException.class,
                () -> quizAttemptService.submitQuiz(QUIZ_ID, request));
    }

    @Test
    void submitQuiz_whenQuestionIdNotBelongToQuiz_shouldThrowIllegalArgumentException() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));

        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of(createAnswerItem(9999L, "Hanoi")));

        assertThrows(IllegalArgumentException.class,
                () -> quizAttemptService.submitQuiz(QUIZ_ID, request));
    }

    @Test
    void submitQuiz_whenDuplicateQuestionId_shouldThrowIllegalArgumentException() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));

        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of(
                createAnswerItem(1001L, "Hanoi"),
                createAnswerItem(1001L, "Saigon")));

        assertThrows(IllegalArgumentException.class,
                () -> quizAttemptService.submitQuiz(QUIZ_ID, request));
    }

    @Test
    void submitQuiz_whenAnswerNotInOptions_shouldThrowIllegalArgumentException() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));

        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of(createAnswerItem(1001L, "Hue")));

        assertThrows(IllegalArgumentException.class,
                () -> quizAttemptService.submitQuiz(QUIZ_ID, request));
    }

    @Test
    void submitQuiz_whenAnswerIsBlank_shouldThrowIllegalArgumentException() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));

        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of(createAnswerItem(1001L, "   ")));

        assertThrows(IllegalArgumentException.class,
                () -> quizAttemptService.submitQuiz(QUIZ_ID, request));
    }

    @Test
    void submitQuiz_happyPath_shouldCalculateScoreCorrectly() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1, question2));

        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of(
                createAnswerItem(1001L, "Hanoi"),
                createAnswerItem(1002L, "Dog")));

        QuizResultResponse response = quizAttemptService.submitQuiz(QUIZ_ID, request);

        assertNotNull(response);
        assertEquals(2, response.getTotalQuestions());
        assertEquals(1, response.getCorrectAnswers());
        assertEquals(50, response.getScore());
    }

    @Test
    void submitQuiz_answerNotExactOptionMatch_shouldThrowEvenIfSemanticEqual() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));

        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of(createAnswerItem(1001L, "  hAnOi  ")));

        assertThrows(IllegalArgumentException.class,
                () -> quizAttemptService.submitQuiz(QUIZ_ID, request));
    }

    @Test
    void submitQuiz_correctAnswerFieldCaseMismatchWithOptions_shouldStillCountAsCorrect() {
        mockSuccessfulUserAndQuiz();

        QuizQuestion inconsistentQuestion = new QuizQuestion();
        inconsistentQuestion.setId(1001L);
        inconsistentQuestion.setQuiz(testQuiz);
        inconsistentQuestion.setCorrectAnswer("  HANOI  ");
        inconsistentQuestion.setOptions(List.of("Hanoi", "Saigon", "Danang"));

        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(inconsistentQuestion));

        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of(createAnswerItem(1001L, "Hanoi")));

        QuizResultResponse response = quizAttemptService.submitQuiz(QUIZ_ID, request);

        assertEquals(1, response.getCorrectAnswers());
        assertEquals(100, response.getScore());
    }

    @Test
    void submitQuiz_whenAnswersListIsNull_shouldReturnZeroScoreWithoutThrowing() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));

        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(null);

        QuizResultResponse response = quizAttemptService.submitQuiz(QUIZ_ID, request);

        assertEquals(1, response.getTotalQuestions());
        assertEquals(0, response.getCorrectAnswers());
        assertEquals(0, response.getScore());
    }

    @Test
    void submitQuiz_whenQuizHasNoQuestions_shouldReturnZeroScoreAvoidingDivisionByZero() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of());

        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of());

        QuizResultResponse response = quizAttemptService.submitQuiz(QUIZ_ID, request);

        assertEquals(0, response.getTotalQuestions());
        assertEquals(0, response.getCorrectAnswers());
        assertEquals(0, response.getScore());
    }

    @Test
    void submitQuiz_shouldSaveQuizAttemptWithCorrectFields() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));

        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of(createAnswerItem(1001L, "Hanoi")));

        quizAttemptService.submitQuiz(QUIZ_ID, request);

        ArgumentCaptor<QuizAttempt> attemptCaptor = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(quizAttemptRepository).save(attemptCaptor.capture());

        QuizAttempt savedAttempt = attemptCaptor.getValue();
        assertEquals(testUser, savedAttempt.getUser());
        assertEquals(testQuiz, savedAttempt.getQuiz());
        assertEquals(100, savedAttempt.getScore());
        assertEquals(1, savedAttempt.getCorrectAnswers());
        assertEquals(1, savedAttempt.getTotalQuestions());
        assertNotNull(savedAttempt.getCompletedAt());
    }

    // ------------------------------------------------------------------
    // updateProgressAfterAttempt()
    // ------------------------------------------------------------------

    @Test
    void submitQuiz_whenNoExistingUserProgress_shouldCreateNewOne() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));
        when(userProgressRepository.findByUser_IdAndTopic_Id(1L, 10L)).thenReturn(Optional.empty());

        SubmitQuizRequest request = new SubmitQuizRequest();

        quizAttemptService.submitQuiz(QUIZ_ID, request);

        ArgumentCaptor<UserProgress> progressCaptor = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository).save(progressCaptor.capture());

        UserProgress savedProgress = progressCaptor.getValue();
        assertNull(savedProgress.getId());
        assertEquals(testUser, savedProgress.getUser());
        assertEquals(testTopic, savedProgress.getTopic());
    }

    @Test
    void submitQuiz_whenExistingUserProgress_shouldUpdateInPlace() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));

        UserProgress existingProgress = new UserProgress();
        existingProgress.setId(500L);
        existingProgress.setUser(testUser);
        existingProgress.setTopic(testTopic);
        existingProgress.setProgressPercent(0);

        when(userProgressRepository.findByUser_IdAndTopic_Id(1L, 10L)).thenReturn(Optional.of(existingProgress));

        SubmitQuizRequest request = new SubmitQuizRequest();

        quizAttemptService.submitQuiz(QUIZ_ID, request);

        ArgumentCaptor<UserProgress> progressCaptor = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository).save(progressCaptor.capture());

        UserProgress updatedProgress = progressCaptor.getValue();
        assertEquals(500L, updatedProgress.getId());
    }

    @Test
    void submitQuiz_whenAchievedEqualsTotalQuizCount_shouldSetStatusCompleted() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));

        when(quizAttemptRepository.countAchievedQuizzesInTopic(eq(1L), eq(10L), anyInt())).thenReturn(2L);
        when(quizRepository.countByTopicId(10L)).thenReturn(2L);

        SubmitQuizRequest request = new SubmitQuizRequest();

        quizAttemptService.submitQuiz(QUIZ_ID, request);

        ArgumentCaptor<UserProgress> progressCaptor = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository).save(progressCaptor.capture());

        UserProgress progress = progressCaptor.getValue();
        assertEquals(100, progress.getProgressPercent());
        assertEquals(ProgressStatus.COMPLETED, progress.getStatus());
    }

    @Test
    void submitQuiz_whenAchievedLessThanTotalQuizCount_shouldSetStatusInProgress() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));

        when(quizAttemptRepository.countAchievedQuizzesInTopic(eq(1L), eq(10L), anyInt())).thenReturn(1L);
        when(quizRepository.countByTopicId(10L)).thenReturn(2L);

        SubmitQuizRequest request = new SubmitQuizRequest();

        quizAttemptService.submitQuiz(QUIZ_ID, request);

        ArgumentCaptor<UserProgress> progressCaptor = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository).save(progressCaptor.capture());

        UserProgress progress = progressCaptor.getValue();
        assertEquals(50, progress.getProgressPercent());
        assertEquals(ProgressStatus.IN_PROGRESS, progress.getStatus());
    }

    @Test
    void submitQuiz_whenTopicHasNoQuiz_shouldSetPercentZeroAndStatusInProgress() {
        mockSuccessfulUserAndQuiz();
        when(quizQuestionRepository.findByQuizId(QUIZ_ID)).thenReturn(List.of(question1));

        when(quizAttemptRepository.countAchievedQuizzesInTopic(eq(1L), eq(10L), anyInt())).thenReturn(0L);
        when(quizRepository.countByTopicId(10L)).thenReturn(0L);

        SubmitQuizRequest request = new SubmitQuizRequest();

        quizAttemptService.submitQuiz(QUIZ_ID, request);

        ArgumentCaptor<UserProgress> progressCaptor = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository).save(progressCaptor.capture());

        UserProgress progress = progressCaptor.getValue();
        assertEquals(0, progress.getProgressPercent());
        assertEquals(ProgressStatus.IN_PROGRESS, progress.getStatus());
    }

    // ------------------------------------------------------------------
    // getMyAttempts()
    // ------------------------------------------------------------------

    @Test
    void getMyAttempts_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(ResourceNotFoundException.class,
                () -> quizAttemptService.getMyAttempts(QUIZ_ID, pageable));
    }

    @Test
    void getMyAttempts_happyPath_shouldReturnMappedPage() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(1000L);
        attempt.setUser(testUser);
        attempt.setQuiz(testQuiz);
        attempt.setScore(80);
        attempt.setCorrectAnswers(4);
        attempt.setTotalQuestions(5);
        attempt.setCompletedAt(Instant.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<QuizAttempt> mockPage = new PageImpl<>(List.of(attempt), pageable, 1);

        when(quizAttemptRepository.findByUser_IdAndQuiz_Id(1L, QUIZ_ID, pageable)).thenReturn(mockPage);

        Page<QuizAttemptResponse> result = quizAttemptService.getMyAttempts(QUIZ_ID, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(80, result.getContent().get(0).getScore());
    }
}