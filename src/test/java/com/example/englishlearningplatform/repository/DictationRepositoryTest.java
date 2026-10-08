package com.example.englishlearningplatform.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.example.englishlearningplatform.dto.dictation.DictationSort;
import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.DictationLesson;
import com.example.englishlearningplatform.entity.DictationResult;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DictationRepositoryTest extends PostgresIntegrationTestBase {

    @Autowired
    DictationLessonRepository lessonRepo;
    @Autowired
    DictationResultRepository resultRepo;
    @Autowired
    TopicRepository topicRepo;
    @Autowired
    UserRepository userRepo;

    @PersistenceContext
    EntityManager em;

    // ═══════════ Helpers ═══════════

    private Topic newTopic() {
        return newTopic(ContentStatus.PUBLISHED);
    }

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

    private DictationLesson newLesson(Topic topic, String title) {
        return newLesson(topic, title, ContentStatus.PUBLISHED);
    }

    private DictationLesson newLesson(Topic topic, ContentStatus status) {
        return newLesson(topic, "Lesson " + UUID.randomUUID(), status);
    }

    private DictationLesson newLesson(Topic topic, String title, ContentStatus status) {
        DictationLesson l = new DictationLesson();
        l.setTopic(topic);
        l.setTitle(title);
        l.setTranscript("some transcript");
        l.setLevel(Level.BEGINNER);
        l.setMediaUrl(title + ".mp3");
        l.setStatus(status);
        return lessonRepo.saveAndFlush(l);
    }

    private DictationResult attempt(User user, DictationLesson lesson, double accuracy, Instant at) {
        DictationResult r = new DictationResult();
        r.setUser(user);
        r.setLesson(lesson);
        r.setUserInput("my answer");
        r.setAccuracy(accuracy);
        r.setCreatedAt(at);
        return resultRepo.saveAndFlush(r);
    }

    private DictationResult attempt(User user, DictationLesson lesson, Instant at) {
        return attempt(user, lesson, 80.0, at);
    }

    private Page<DictationLesson> findRecent(Topic topic, User user) {
        Specification<DictationLesson> spec = DictationLessonSpecifications.catalog(
                user.getId(), null, topic.getId(), null, null, DictationSort.RECENT);
        return lessonRepo.findAll(spec, PageRequest.of(0, 10));
    }

    // ═══════════ Derived Query Tests ═══════════

    @Test
    void findByTopic_IdAndMediaUrlIsNotNullAndStatusOrderByIdAsc_returnsOnlyPublishedWithAudio() {
        Topic topic = newTopic(ContentStatus.PUBLISHED);

        DictationLesson validLesson = newLesson(topic, "Valid Lesson", ContentStatus.PUBLISHED);

        newLesson(topic, "Draft Lesson", ContentStatus.DRAFT);
        newLesson(topic, "Archived Lesson", ContentStatus.ARCHIVED);

        DictationLesson noAudioLesson = new DictationLesson();
        noAudioLesson.setTopic(topic);
        noAudioLesson.setTitle("No Audio Lesson");
        noAudioLesson.setTranscript("transcript");
        noAudioLesson.setLevel(Level.BEGINNER);
        noAudioLesson.setMediaUrl(null);
        noAudioLesson.setStatus(ContentStatus.PUBLISHED);
        lessonRepo.saveAndFlush(noAudioLesson);

        List<DictationLesson> result = lessonRepo.findByTopic_IdAndMediaUrlIsNotNullAndStatusOrderByIdAsc(
                topic.getId(), ContentStatus.PUBLISHED);

        assertThat(result)
                .extracting(DictationLesson::getId)
                .containsExactly(validLesson.getId());
    }

    @Test
    void findByIdAndStatusAndTopic_Status_returnsLessonOnlyWhenBothArePublished() {
        Topic publishedTopic = newTopic(ContentStatus.PUBLISHED);
        Topic draftTopic = newTopic(ContentStatus.DRAFT);
        Topic archivedTopic = newTopic(ContentStatus.ARCHIVED);

        DictationLesson validLesson = newLesson(publishedTopic, ContentStatus.PUBLISHED);

        DictationLesson draftLesson = newLesson(publishedTopic, ContentStatus.DRAFT);
        DictationLesson archivedLesson = newLesson(publishedTopic, ContentStatus.ARCHIVED);

        DictationLesson lessonInDraftTopic = newLesson(draftTopic, ContentStatus.PUBLISHED);
        DictationLesson lessonInArchivedTopic = newLesson(archivedTopic, ContentStatus.PUBLISHED);

        Optional<DictationLesson> found = lessonRepo.findByIdAndStatusAndTopic_Status(
                validLesson.getId(), ContentStatus.PUBLISHED, ContentStatus.PUBLISHED);
        assertThat(found).isPresent();

        assertThat(lessonRepo.findByIdAndStatusAndTopic_Status(
                draftLesson.getId(), ContentStatus.PUBLISHED, ContentStatus.PUBLISHED)).isEmpty();
        assertThat(lessonRepo.findByIdAndStatusAndTopic_Status(
                archivedLesson.getId(), ContentStatus.PUBLISHED, ContentStatus.PUBLISHED)).isEmpty();

        assertThat(lessonRepo.findByIdAndStatusAndTopic_Status(
                lessonInDraftTopic.getId(), ContentStatus.PUBLISHED, ContentStatus.PUBLISHED)).isEmpty();
        assertThat(lessonRepo.findByIdAndStatusAndTopic_Status(
                lessonInArchivedTopic.getId(), ContentStatus.PUBLISHED, ContentStatus.PUBLISHED)).isEmpty();
    }

    // ═══════════ Test catalog Status Filters (Bước 2b) ═══════════

    @Test
    void catalog_hidesDraftAndArchivedLessons() {
        Topic topic = newTopic(ContentStatus.PUBLISHED);
        newLesson(topic, ContentStatus.DRAFT);
        DictationLesson publishedLesson = newLesson(topic, ContentStatus.PUBLISHED);
        newLesson(topic, ContentStatus.ARCHIVED);

        Specification<DictationLesson> spec = DictationLessonSpecifications.catalog(
                null, null, topic.getId(), null, null, null);
        List<DictationLesson> result = lessonRepo.findAll(spec);

        assertThat(result).extracting(DictationLesson::getId).containsExactly(publishedLesson.getId());
    }

    @Test
    void catalog_hidesPublishedLessonOfNonPublishedTopic() {
        Topic draftTopic = newTopic(ContentStatus.DRAFT);
        newLesson(draftTopic, ContentStatus.PUBLISHED);

        Specification<DictationLesson> spec = DictationLessonSpecifications.catalog(
                null, null, draftTopic.getId(), null, null, null);
        List<DictationLesson> result = lessonRepo.findAll(spec);

        assertThat(result).isEmpty();
    }

    @Test
    void catalog_hidesPublishedLessonWithoutAudio() {
        Topic topic = newTopic(ContentStatus.PUBLISHED);

        DictationLesson lessonNoAudio = new DictationLesson();
        lessonNoAudio.setTopic(topic);
        lessonNoAudio.setTitle("No Audio Lesson");
        lessonNoAudio.setTranscript("some transcript");
        lessonNoAudio.setLevel(Level.BEGINNER);
        lessonNoAudio.setMediaUrl(null); // Không có audio URL
        lessonNoAudio.setStatus(ContentStatus.PUBLISHED);
        lessonRepo.saveAndFlush(lessonNoAudio);

        Specification<DictationLesson> spec = DictationLessonSpecifications.catalog(
                null, null, topic.getId(), null, null, null);
        List<DictationLesson> result = lessonRepo.findAll(spec);

        assertThat(result).isEmpty();
    }

    // ═══════════ Test 1: sort=recent ═══════════

    @Test
    void recent_ordersByLatestAttempt_thenNeverAttempted() {
        Topic topic = newTopic();
        User user = newUser();

        DictationLesson earlier = newLesson(topic, "earlier");
        newLesson(topic, "never");
        DictationLesson recent = newLesson(topic, "recent");

        Instant now = Instant.now();
        attempt(user, earlier, now.minus(2, ChronoUnit.DAYS));
        attempt(user, recent, now.minus(1, ChronoUnit.HOURS));

        Page<DictationLesson> page = findRecent(topic, user);

        assertThat(page.getContent()).extracting(DictationLesson::getTitle)
                .containsExactly("recent", "earlier", "never");
    }

    @Test
    void recent_ignoresOtherUsersAttempts() {
        Topic topic = newTopic();
        User me = newUser();
        User other = newUser();

        DictationLesson a = newLesson(topic, "a");
        DictationLesson b = newLesson(topic, "b");
        newLesson(topic, "c");

        Instant now = Instant.now();
        attempt(me, a, now.minus(2, ChronoUnit.DAYS));
        attempt(other, b, now.minus(1, ChronoUnit.MINUTES));

        Page<DictationLesson> page = findRecent(topic, me);

        assertThat(page.getContent()).extracting(DictationLesson::getTitle)
                .containsExactly("a", "c", "b");
    }

    @Test
    void recent_lessonAttemptedTwice_appearsOnce_orderedByLatestAttempt() {
        Topic topic = newTopic();
        User user = newUser();

        DictationLesson x = newLesson(topic, "x");
        newLesson(topic, "z");
        DictationLesson y = newLesson(topic, "y");

        Instant now = Instant.now();
        attempt(user, y, now.minus(3, ChronoUnit.DAYS));
        attempt(user, y, now.minus(1, ChronoUnit.HOURS));
        attempt(user, x, now.minus(2, ChronoUnit.DAYS));

        Page<DictationLesson> page = findRecent(topic, user);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(DictationLesson::getTitle)
                .containsExactly("y", "x", "z");
    }

    @Test
    void deleteTopic_cascadesToLessons() {
        Topic topic = newTopic();
        Long topicId = topic.getId();
        Long lessonId = newLesson(topic, "to-be-cascaded").getId();

        em.clear();

        topicRepo.deleteById(topicId);
        topicRepo.flush();
        em.clear();

        assertThat(lessonRepo.findById(lessonId)).isEmpty();
    }

    @Test
    void deleteLesson_withResults_isRejectedByDatabase() {
        Topic topic = newTopic();
        User user = newUser();
        DictationLesson lesson = newLesson(topic, "has-result");
        attempt(user, lesson, Instant.now());
        Long lessonId = lesson.getId();

        em.clear();

        assertThatThrownBy(() -> {
            lessonRepo.deleteById(lessonId);
            lessonRepo.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteTopic_whenAnyLessonHasResults_isRejectedByDatabase() {
        Topic topic = newTopic();
        User user = newUser();
        DictationLesson withResult = newLesson(topic, "with-result");
        newLesson(topic, "without-result");
        attempt(user, withResult, Instant.now());
        Long topicId = topic.getId();

        em.clear();

        assertThatThrownBy(() -> {
            topicRepo.deleteById(topicId);
            topicRepo.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void stats_aggregatesOnlyCurrentUsersAttempts() {
        Topic topic = newTopic();
        User me = newUser();
        User other = newUser();

        DictationLesson x = newLesson(topic, "x");
        DictationLesson y = newLesson(topic, "y");

        Instant base = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant latest = base.minus(1, ChronoUnit.HOURS);

        attempt(me, x, 60.0, base.minus(3, ChronoUnit.DAYS));
        attempt(me, x, 100.0, base.minus(2, ChronoUnit.DAYS));
        attempt(me, x, 80.0, latest);
        attempt(other, x, 10.0, base);
        attempt(other, y, 90.0, base);

        List<Long> lessonIds = List.of(x.getId(), y.getId());

        List<DictationLessonStats> mine = resultRepo.findStatsByUserIdAndLessonIds(me.getId(), lessonIds);
        assertThat(mine).extracting(DictationLessonStats::getLessonId).containsExactly(x.getId());
        DictationLessonStats mineX = mine.get(0);
        assertThat(mineX.getAttempts()).isEqualTo(3L);
        assertThat(mineX.getBestAccuracy()).isEqualTo(100.0);
        assertThat(mineX.getLastAttemptAt()).isEqualTo(latest);

        List<DictationLessonStats> theirs = resultRepo.findStatsByUserIdAndLessonIds(other.getId(), lessonIds);
        assertThat(theirs).hasSize(2);
        DictationLessonStats otherX = theirs.stream()
                .filter(s -> s.getLessonId().equals(x.getId())).findFirst().orElseThrow();
        assertThat(otherX.getAttempts()).isEqualTo(1L);
        assertThat(otherX.getBestAccuracy()).isEqualTo(10.0);
    }
}