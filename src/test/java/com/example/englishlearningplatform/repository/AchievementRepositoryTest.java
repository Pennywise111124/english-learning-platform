package com.example.englishlearningplatform.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.example.englishlearningplatform.entity.AchievementCode;
import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.DictationLesson;
import com.example.englishlearningplatform.entity.DictationResult;
import com.example.englishlearningplatform.entity.Flashcard;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Quiz;
import com.example.englishlearningplatform.entity.QuizAttempt;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.entity.UserAchievement;
import com.example.englishlearningplatform.entity.UserVocabulary;
import com.example.englishlearningplatform.entity.VocabularyStatus;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AchievementRepositoryTest extends PostgresIntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-10-11T03:00:00Z");
    private static final int MIN_SCORE = 70;

    @Autowired
    private EntityManager em;
    @Autowired
    private UserAchievementRepository achievementRepository;
    @Autowired
    private QuizAttemptRepository quizAttemptRepository;
    @Autowired
    private UserVocabularyRepository userVocabularyRepository;
    @Autowired
    private DictationResultRepository dictationResultRepository;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User persistUser(String name) {
        User u = new User();
        u.setUsername(name);
        u.setEmail(name + "@test.com");
        u.setPasswordHash("hash");
        em.persist(u);
        em.flush();
        return u;
    }

    private Topic persistTopic(String title) {
        Topic t = new Topic();
        t.setTitle(title);
        t.setLevel(Level.BEGINNER);
        t.setStatus(ContentStatus.PUBLISHED);
        em.persist(t);
        em.flush();
        return t;
    }

    private Quiz persistQuiz(Topic topic, String title, ContentStatus status) {
        Quiz q = new Quiz();
        q.setTopic(topic);
        q.setTitle(title);
        q.setStatus(status);
        em.persist(q);
        em.flush();
        return q;
    }

    private void persistAttempt(User user, Quiz quiz, int score) {
        QuizAttempt a = new QuizAttempt();
        a.setUser(user);
        a.setQuiz(quiz);
        a.setScore(score);
        a.setCorrectAnswers(0);
        a.setTotalQuestions(1);
        a.setCompletedAt(NOW);
        em.persist(a);
        em.flush();
    }

    private Flashcard persistFlashcard(Topic topic, String word) {
        Flashcard f = new Flashcard();
        f.setTopic(topic);
        f.setWord(word);
        f.setMeaning(word + " meaning");
        em.persist(f);
        em.flush();
        return f;
    }

    private void persistVocab(User user, Flashcard flashcard, VocabularyStatus status) {
        UserVocabulary uv = new UserVocabulary();
        uv.setUser(user);
        uv.setFlashcard(flashcard);
        uv.setStatus(status);
        uv.setNextReviewAt(NOW);
        em.persist(uv);
        em.flush();
    }

    private DictationLesson persistLesson(Topic topic, String title) {
        DictationLesson l = new DictationLesson();
        l.setTopic(topic);
        l.setTitle(title);
        l.setTranscript("transcript");
        l.setLevel(Level.BEGINNER);
        l.setStatus(ContentStatus.PUBLISHED);
        em.persist(l);
        em.flush();
        return l;
    }

    private void persistDictationResult(User user, DictationLesson lesson) {
        DictationResult r = new DictationResult();
        r.setUser(user);
        r.setLesson(lesson);
        r.setUserInput("text");
        r.setAccuracy(90.0);
        r.setCreatedAt(NOW);
        em.persist(r);
        em.flush();
    }

    // ------------------------------------------------------------------
    // UserAchievementRepository
    // ------------------------------------------------------------------

    @Test
    void insertIfAbsent_secondCallSameUserAndCode_returnsZeroAndKeepsOneRow() {
        User u = persistUser("a1");

        int first = achievementRepository.insertIfAbsent(u.getId(), "XP_100", NOW);
        int second = achievementRepository.insertIfAbsent(u.getId(), "XP_100", NOW);
        em.flush();
        em.clear();

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
        assertThat(achievementRepository.findByUserId(u.getId())).hasSize(1);
    }

    @Test
    void insertIfAbsent_twoDifferentCodes_createsTwoRows() {
        User u = persistUser("a2");

        achievementRepository.insertIfAbsent(u.getId(), "XP_100", NOW);
        achievementRepository.insertIfAbsent(u.getId(), "STREAK_7", NOW);
        em.flush();
        em.clear();

        assertThat(achievementRepository.findByUserId(u.getId()))
                .extracting(UserAchievement::getCode)
                .containsExactlyInAnyOrder(AchievementCode.XP_100, AchievementCode.STREAK_7);
    }

    @Test
    void dismiss_isIdempotent() {
        User u = persistUser("a3");
        achievementRepository.insertIfAbsent(u.getId(), "XP_100", NOW);
        em.flush();
        em.clear();

        int first = achievementRepository.dismiss(u.getId(), AchievementCode.XP_100, NOW);
        int second = achievementRepository.dismiss(u.getId(), AchievementCode.XP_100, NOW);
        em.flush();
        em.clear();

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
        assertThat(achievementRepository.findByUserId(u.getId()).get(0).getDismissedAt())
                .isEqualTo(NOW);
    }

    @Test
    void dismiss_otherUsersAchievement_returnsZeroAndLeavesRowUntouched() {
        User owner = persistUser("owner");
        User other = persistUser("other");
        achievementRepository.insertIfAbsent(owner.getId(), "XP_100", NOW);
        em.flush();
        em.clear();

        int updated = achievementRepository.dismiss(other.getId(), AchievementCode.XP_100, NOW);
        em.flush();
        em.clear();

        assertThat(updated).isEqualTo(0);
        assertThat(achievementRepository.findByUserId(owner.getId()).get(0).getDismissedAt())
                .isNull();
    }

    @Test
    void dismiss_codeNotUnlocked_returnsZero() {
        User u = persistUser("a5");
        achievementRepository.insertIfAbsent(u.getId(), "XP_100", NOW);
        em.flush();
        em.clear();

        assertThat(achievementRepository.dismiss(u.getId(), AchievementCode.STREAK_7, NOW))
                .isEqualTo(0);
    }

    // ------------------------------------------------------------------
    // countDistinctPassedQuizzes
    // ------------------------------------------------------------------

    @Test
    void countDistinctPassedQuizzes_sameQuizPassedThreeTimes_countsOnce() {
        User u = persistUser("q6");
        Quiz quiz = persistQuiz(persistTopic("T6"), "Q6", ContentStatus.PUBLISHED);
        persistAttempt(u, quiz, 100);
        persistAttempt(u, quiz, 90);
        persistAttempt(u, quiz, 80);
        em.clear();

        assertThat(quizAttemptRepository.countDistinctPassedQuizzes(u.getId(), MIN_SCORE))
                .isEqualTo(1);
    }

    @Test
    void countDistinctPassedQuizzes_scoreEqualToMinCounts_oneBelowDoesNot() {
        User u = persistUser("q7");
        Topic topic = persistTopic("T7");
        persistAttempt(u, persistQuiz(topic, "AtMin", ContentStatus.PUBLISHED), MIN_SCORE);
        persistAttempt(u, persistQuiz(topic, "BelowMin", ContentStatus.PUBLISHED), MIN_SCORE - 1);
        em.clear();

        assertThat(quizAttemptRepository.countDistinctPassedQuizzes(u.getId(), MIN_SCORE))
                .isEqualTo(1);
    }

    @Test
    void countDistinctPassedQuizzes_archivedQuizStillCounts() {
        User u = persistUser("q8");
        Quiz archived = persistQuiz(persistTopic("T8"), "Archived", ContentStatus.ARCHIVED);
        persistAttempt(u, archived, 100);
        em.clear();

        assertThat(quizAttemptRepository.countDistinctPassedQuizzes(u.getId(), MIN_SCORE))
                .isEqualTo(1);
    }

    @Test
    void countDistinctPassedQuizzes_ignoresOtherUsersAttempts() {
        User me = persistUser("q9me");
        User other = persistUser("q9other");
        Topic topic = persistTopic("T9");
        persistAttempt(me, persistQuiz(topic, "Mine", ContentStatus.PUBLISHED), 100);
        persistAttempt(other, persistQuiz(topic, "Theirs1", ContentStatus.PUBLISHED), 100);
        persistAttempt(other, persistQuiz(topic, "Theirs2", ContentStatus.PUBLISHED), 100);
        em.clear();

        assertThat(quizAttemptRepository.countDistinctPassedQuizzes(me.getId(), MIN_SCORE))
                .isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // countByUser_IdAndStatus
    // ------------------------------------------------------------------

    @Test
    void countByUserIdAndStatus_countsOnlyKnownOfThatUser() {
        User me = persistUser("v10me");
        User other = persistUser("v10other");
        Topic topic = persistTopic("T10");
        Flashcard f1 = persistFlashcard(topic, "w1");
        Flashcard f2 = persistFlashcard(topic, "w2");
        Flashcard f3 = persistFlashcard(topic, "w3");
        Flashcard f4 = persistFlashcard(topic, "w4");

        persistVocab(me, f1, VocabularyStatus.KNOWN);
        persistVocab(me, f2, VocabularyStatus.KNOWN);
        persistVocab(me, f3, VocabularyStatus.LEARNING);
        persistVocab(me, f4, VocabularyStatus.NEW);
        persistVocab(other, f1, VocabularyStatus.KNOWN);
        em.clear();

        assertThat(userVocabularyRepository.countByUser_IdAndStatus(me.getId(), VocabularyStatus.KNOWN))
                .isEqualTo(2);
    }

    // ------------------------------------------------------------------
    // countDistinctLessonsByUserId
    // ------------------------------------------------------------------

    @Test
    void countDistinctLessons_sameLessonFiveTimes_countsOnce() {
        User u = persistUser("d11");
        DictationLesson lesson = persistLesson(persistTopic("T11"), "L11");
        for (int i = 0; i < 5; i++) {
            persistDictationResult(u, lesson);
        }
        em.clear();

        assertThat(dictationResultRepository.countDistinctLessonsByUserId(u.getId()))
                .isEqualTo(1);
    }

    @Test
    void countDistinctLessons_ignoresOtherUsers() {
        User me = persistUser("d12me");
        User other = persistUser("d12other");
        Topic topic = persistTopic("T12");
        DictationLesson shared = persistLesson(topic, "Shared");
        DictationLesson onlyOther = persistLesson(topic, "OnlyOther");

        persistDictationResult(me, shared);
        persistDictationResult(other, shared);
        persistDictationResult(other, onlyOther);
        em.clear();

        assertThat(dictationResultRepository.countDistinctLessonsByUserId(me.getId()))
                .isEqualTo(1);
    }
}