package com.example.englishlearningplatform.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Quiz;
import com.example.englishlearningplatform.entity.Topic;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuizRepositoryTest extends PostgresIntegrationTestBase {

        @Autowired
        QuizRepository quizRepo;
        @Autowired
        TopicRepository topicRepo;
        @Autowired
        JdbcTemplate jdbc;

        private Topic newTopic(ContentStatus status) {
                Topic t = new Topic();
                t.setTitle("Topic " + UUID.randomUUID());
                t.setDescription("desc");
                t.setLevel(Level.BEGINNER);
                t.setStatus(status);
                return topicRepo.saveAndFlush(t);
        }

        private Topic newTopic() {
                return newTopic(ContentStatus.PUBLISHED);
        }

        private Quiz newQuiz(Topic topic, String title, ContentStatus status) {
                Quiz q = new Quiz();
                q.setTopic(topic);
                q.setTitle(title);
                q.setStatus(status);
                return q;
        }

        private Quiz newQuiz(Topic topic, String title) {
                return newQuiz(topic, title, ContentStatus.PUBLISHED);
        }

        @Test
        void quizTitle_isUniquePerTopic_caseInsensitive() {
                Topic topic = newTopic();
                quizRepo.saveAndFlush(newQuiz(topic, "Quiz A"));

                assertThatThrownBy(() -> quizRepo.saveAndFlush(newQuiz(topic, "quiz a")))
                                .isInstanceOf(DataIntegrityViolationException.class)
                                .rootCause().hasMessageContaining("uk_quizzes_topic_title_lower");
        }

        @Test
        void quizTitle_sameTitleInDifferentTopic_isAllowed() {
                Topic topic = newTopic();
                Topic otherTopic = newTopic();

                quizRepo.saveAndFlush(newQuiz(topic, "Quiz A"));

                assertThatCode(() -> quizRepo.saveAndFlush(newQuiz(otherTopic, "Quiz A")))
                                .doesNotThrowAnyException();
        }

        @Test
        void status_rejectsValuesOutsideTheEnum() {
                assertThatThrownBy(() -> jdbc.update(
                                "INSERT INTO topics (title, level, status) VALUES (?, ?, ?)",
                                "bogus-" + UUID.randomUUID(), "BEGINNER", "BOGUS"))
                                .isInstanceOf(DataIntegrityViolationException.class)
                                .rootCause().hasMessageContaining("ck_topics_status");
        }

        @Test
        void status_hasNoDatabaseDefault() {
                assertThatThrownBy(() -> jdbc.update(
                                "INSERT INTO topics (title, level) VALUES (?, ?)",
                                "nodefault-" + UUID.randomUUID(), "BEGINNER"))
                                .isInstanceOf(DataIntegrityViolationException.class)
                                .rootCause().hasMessageContaining("status");
        }

        // CHỈ lọc Quiz. Service phải tự kiểm tra Topic PUBLISHED trước
        @Test
        void findByTopic_IdAndStatus_returnsOnlyPublishedQuizzes() {
                Topic publishedTopic = newTopic(ContentStatus.PUBLISHED);

                Quiz publishedQuiz = quizRepo
                                .saveAndFlush(newQuiz(publishedTopic, "Published Quiz", ContentStatus.PUBLISHED));
                quizRepo.saveAndFlush(newQuiz(publishedTopic, "Draft Quiz", ContentStatus.DRAFT));
                quizRepo.saveAndFlush(newQuiz(publishedTopic, "Archived Quiz", ContentStatus.ARCHIVED));

                List<Quiz> result = quizRepo.findByTopic_IdAndStatus(publishedTopic.getId(), ContentStatus.PUBLISHED);

                assertThat(result)
                                .extracting(Quiz::getId)
                                .containsExactly(publishedQuiz.getId());
        }

        @Test
        void findByIdAndStatusAndTopic_Status_returnsQuizOnlyWhenBothArePublished() {
                Topic publishedTopic = newTopic(ContentStatus.PUBLISHED);
                Topic draftTopic = newTopic(ContentStatus.DRAFT);
                Topic archivedTopic = newTopic(ContentStatus.ARCHIVED);

                Quiz publishedQuiz = quizRepo
                                .saveAndFlush(newQuiz(publishedTopic, "Valid Quiz", ContentStatus.PUBLISHED));

                Quiz draftQuiz = quizRepo.saveAndFlush(newQuiz(publishedTopic, "Draft Quiz", ContentStatus.DRAFT));
                Quiz archivedQuiz = quizRepo
                                .saveAndFlush(newQuiz(publishedTopic, "Archived Quiz", ContentStatus.ARCHIVED));

                Quiz quizInDraftTopic = quizRepo
                                .saveAndFlush(newQuiz(draftTopic, "Quiz in Draft Topic", ContentStatus.PUBLISHED));
                Quiz quizInArchivedTopic = quizRepo
                                .saveAndFlush(newQuiz(archivedTopic, "Quiz in Archived Topic",
                                                ContentStatus.PUBLISHED));

                Optional<Quiz> found = quizRepo.findByIdAndStatusAndTopic_Status(
                                publishedQuiz.getId(), ContentStatus.PUBLISHED, ContentStatus.PUBLISHED);
                assertThat(found).isPresent();

                assertThat(quizRepo.findByIdAndStatusAndTopic_Status(
                                draftQuiz.getId(), ContentStatus.PUBLISHED, ContentStatus.PUBLISHED)).isEmpty();
                assertThat(quizRepo.findByIdAndStatusAndTopic_Status(
                                archivedQuiz.getId(), ContentStatus.PUBLISHED, ContentStatus.PUBLISHED)).isEmpty();

                assertThat(quizRepo.findByIdAndStatusAndTopic_Status(
                                quizInDraftTopic.getId(), ContentStatus.PUBLISHED, ContentStatus.PUBLISHED)).isEmpty();
                assertThat(quizRepo.findByIdAndStatusAndTopic_Status(
                                quizInArchivedTopic.getId(), ContentStatus.PUBLISHED, ContentStatus.PUBLISHED))
                                .isEmpty();
        }
}