package com.example.englishlearningplatform.service;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Quiz;
import com.example.englishlearningplatform.entity.QuizQuestion;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.exception.ResourceConflictException;
import com.example.englishlearningplatform.repository.PostgresIntegrationTestBase;
import com.example.englishlearningplatform.repository.QuizQuestionRepository;
import com.example.englishlearningplatform.repository.QuizRepository;
import com.example.englishlearningplatform.repository.TopicRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QuizService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class QuizServiceConcurrencyTest extends PostgresIntegrationTestBase {

    @Autowired
    QuizService quizService;
    @Autowired
    QuizRepository quizRepo;
    @Autowired
    QuizQuestionRepository questionRepo;
    @Autowired
    TopicRepository topicRepo;
    @Autowired
    PlatformTransactionManager txManager;

    private TransactionTemplate tx;
    private ExecutorService executor;
    private Long topicId;
    private Long quizId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void cleanUp() {
        executor.shutdownNow();
        if (quizId != null) {
            questionRepo.deleteAll(questionRepo.findByQuizId(quizId));
            quizRepo.deleteById(quizId);
        }
        if (topicId != null) {
            topicRepo.deleteById(topicId);
        }
    }

    // ═══════════ Helpers ═══════════

    private Topic newTopic() {
        Topic t = new Topic();
        t.setTitle("Topic " + UUID.randomUUID());
        t.setDescription("desc");
        t.setLevel(Level.BEGINNER);
        t.setStatus(ContentStatus.PUBLISHED);
        Topic saved = topicRepo.saveAndFlush(t);
        topicId = saved.getId();
        return saved;
    }

    private Quiz newQuiz(Topic topic, ContentStatus status) {
        Quiz q = new Quiz();
        q.setTopic(topic);
        q.setTitle("Quiz " + UUID.randomUUID());
        q.setStatus(status);
        Quiz saved = quizRepo.saveAndFlush(q);
        quizId = saved.getId();
        return saved;
    }

    private QuizQuestion newQuestion(Quiz quiz, String text) {
        QuizQuestion q = new QuizQuestion();
        q.setQuiz(quiz);
        q.setQuestion(text);
        q.setOptions(List.of("a", "b"));
        q.setCorrectAnswer("a");
        return questionRepo.saveAndFlush(q);
    }

    private Future<?> holdLockThenDelete(Long questionId, CountDownLatch lockHeld, CountDownLatch release) {
        return executor.submit(() -> tx.executeWithoutResult(s -> {
            quizRepo.findByIdForUpdate(quizId).orElseThrow();
            lockHeld.countDown();
            try {
                if (!release.await(10, SECONDS)) {
                    throw new IllegalStateException("release timeout");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            questionRepo.deleteById(questionId);
        }));
    }

    // ═══════════ Tests ═══════════

    @Test
    void deleteQuestion_waitsForQuizLock_thenSeesCommittedCount() throws Exception {
        Quiz quiz = newQuiz(newTopic(), ContentStatus.PUBLISHED);
        Long questionAId = newQuestion(quiz, "A").getId();
        Long questionBId = newQuestion(quiz, "B").getId();

        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<?> holder = holdLockThenDelete(questionBId, lockHeld, release);
        assertThat(lockHeld.await(5, SECONDS)).isTrue();

        Future<?> deleter = executor.submit(() -> quizService.deleteQuestion(questionAId));

        assertThatThrownBy(() -> deleter.get(1500, MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        release.countDown();
        holder.get(5, SECONDS);

        assertThatThrownBy(() -> deleter.get(5, SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ResourceConflictException.class);

        assertThat(questionRepo.countByQuizId(quizId)).isEqualTo(1);
        assertThat(questionRepo.existsById(questionAId)).isTrue();
    }

    @Test
    void changeStatus_publish_waitsForQuizLock_thenRejectsEmptyQuiz() throws Exception {
        Quiz quiz = newQuiz(newTopic(), ContentStatus.DRAFT);
        Long onlyQuestionId = newQuestion(quiz, "only").getId();

        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<?> holder = holdLockThenDelete(onlyQuestionId, lockHeld, release);
        assertThat(lockHeld.await(5, SECONDS)).isTrue();

        Future<?> publisher = executor.submit(
                () -> quizService.changeStatus(quizId, ContentStatus.PUBLISHED));

        assertThatThrownBy(() -> publisher.get(1500, MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        release.countDown();
        holder.get(5, SECONDS);

        assertThatThrownBy(() -> publisher.get(5, SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ResourceConflictException.class)
                .hasRootCauseMessage("Cannot publish a quiz without questions");

        assertThat(quizRepo.findById(quizId).orElseThrow().getStatus())
                .isEqualTo(ContentStatus.DRAFT);
    }
}