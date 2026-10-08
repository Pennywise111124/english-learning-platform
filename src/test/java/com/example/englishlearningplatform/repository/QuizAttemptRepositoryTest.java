package com.example.englishlearningplatform.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Quiz;
import com.example.englishlearningplatform.entity.QuizAttempt;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.entity.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuizAttemptRepositoryTest extends PostgresIntegrationTestBase {

    private static final int PASSING_SCORE = 70;

    @Autowired
    QuizAttemptRepository attemptRepo;
    @Autowired
    QuizRepository quizRepo;
    @Autowired
    TopicRepository topicRepo;
    @Autowired
    UserRepository userRepo;

    // ═══════════ Helpers ═══════════

    private Topic newTopic(ContentStatus status) {
        Topic t = new Topic();
        t.setTitle("Topic " + UUID.randomUUID());
        t.setDescription("desc");
        t.setLevel(Level.BEGINNER);
        t.setStatus(status);
        return topicRepo.saveAndFlush(t);
    }

    private User newUser() {
        User u = new User();
        u.setUsername("user-" + UUID.randomUUID());
        u.setEmail(UUID.randomUUID() + "@test.local");
        u.setPasswordHash("hash");
        return userRepo.saveAndFlush(u);
    }

    private Quiz newQuiz(Topic topic, String title, ContentStatus status) {
        Quiz q = new Quiz();
        q.setTopic(topic);
        q.setTitle(title);
        q.setStatus(status);
        return quizRepo.saveAndFlush(q);
    }

    private QuizAttempt attempt(User user, Quiz quiz, int score) {
        QuizAttempt a = new QuizAttempt();
        a.setUser(user);
        a.setQuiz(quiz);
        a.setScore(score);
        a.setTotalQuestions(10);
        a.setCorrectAnswers(score / 10);
        a.setCompletedAt(Instant.now());
        return attemptRepo.saveAndFlush(a);
    }

    private long achieved(User user, Topic topic) {
        return attemptRepo.countAchievedQuizzesInTopic(
                user.getId(), topic.getId(), PASSING_SCORE, ContentStatus.PUBLISHED);
    }

    // ═══════════ Tests ═══════════

    @Test
    void countAchieved_countsOnlyPublishedQuizzesAtOrAboveThreshold() {
        Topic topic = newTopic(ContentStatus.PUBLISHED);
        User user = newUser();

        Quiz a = newQuiz(topic, "A", ContentStatus.PUBLISHED);
        Quiz b = newQuiz(topic, "B", ContentStatus.PUBLISHED);
        Quiz c = newQuiz(topic, "C", ContentStatus.ARCHIVED);
        Quiz d = newQuiz(topic, "D", ContentStatus.PUBLISHED);

        attempt(user, a, 90);
        attempt(user, b, 60);
        attempt(user, c, 100);
        attempt(user, d, 70);

        assertThat(achieved(user, topic)).isEqualTo(2);
    }

    @Test
    void countAchieved_countsDistinctQuizzes_notAttempts() {
        Topic topic = newTopic(ContentStatus.PUBLISHED);
        User user = newUser();
        Quiz only = newQuiz(topic, "Only", ContentStatus.PUBLISHED);

        attempt(user, only, 80);
        attempt(user, only, 90);
        attempt(user, only, 100);

        long achieved = achieved(user, topic);
        long total = quizRepo.countByTopic_IdAndStatus(topic.getId(), ContentStatus.PUBLISHED);

        assertThat(achieved).isEqualTo(1);
        assertThat(achieved).isLessThanOrEqualTo(total);
        assertThat(achieved * 100 / total).isEqualTo(100);
    }

    @Test
    void countAchieved_quizCountsIfAnyAttemptReachesThreshold() {
        Topic topic = newTopic(ContentStatus.PUBLISHED);
        User user = newUser();
        Quiz improved = newQuiz(topic, "Improved", ContentStatus.PUBLISHED);
        Quiz neverPassed = newQuiz(topic, "NeverPassed", ContentStatus.PUBLISHED);

        attempt(user, improved, 40);
        attempt(user, improved, 80);
        attempt(user, neverPassed, 69);
        attempt(user, neverPassed, 50);

        assertThat(achieved(user, topic)).isEqualTo(1);
    }

    @Test
    void countAchieved_ignoresOtherUsersAndOtherTopics() {
        Topic topic = newTopic(ContentStatus.PUBLISHED);
        Topic otherTopic = newTopic(ContentStatus.PUBLISHED);
        User me = newUser();
        User other = newUser();

        Quiz mine = newQuiz(topic, "Mine", ContentStatus.PUBLISHED);
        Quiz elsewhere = newQuiz(otherTopic, "Elsewhere", ContentStatus.PUBLISHED);

        attempt(me, mine, 90);
        attempt(other, mine, 100);
        attempt(me, elsewhere, 100);

        assertThat(achieved(me, topic)).isEqualTo(1);
        assertThat(achieved(other, topic)).isEqualTo(1);
        assertThat(achieved(me, otherTopic)).isEqualTo(1);
    }
}