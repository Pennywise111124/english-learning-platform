package com.example.englishlearningplatform.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.example.englishlearningplatform.dto.topic.TopicSort;
import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.ProgressStatus;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.entity.UserProgress;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TopicRepositoryTest extends PostgresIntegrationTestBase {

    @Autowired
    TopicRepository topicRepo;
    @Autowired
    UserProgressRepository progressRepo;
    @Autowired
    UserRepository userRepo;

    private Topic newTopic(String title) {
        return newTopic(title, ContentStatus.PUBLISHED);
    }

    private Topic newTopic(String title, ContentStatus status) {
        Topic t = new Topic();
        t.setTitle(title + " " + UUID.randomUUID());
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

    private UserProgress learn(User user, Topic topic) {
        UserProgress p = new UserProgress();
        p.setUser(user);
        p.setTopic(topic);
        p.setStatus(ProgressStatus.IN_PROGRESS);
        p.setProgressPercent(0);
        return progressRepo.saveAndFlush(p);
    }

    private void learnedBy(int count, Topic topic) {
        for (int i = 0; i < count; i++) {
            learn(newUser(), topic);
        }
    }

    // ═══════════ NEW: Derived Query Tests ═══════════

    @Test
    void existsByIdAndStatus_returnsTrueOnlyWhenStatusMatches() {
        Topic publishedTopic = newTopic("Published Topic", ContentStatus.PUBLISHED);
        Topic draftTopic = newTopic("Draft Topic", ContentStatus.DRAFT);
        Topic archivedTopic = newTopic("Archived Topic", ContentStatus.ARCHIVED);

        assertThat(topicRepo.existsByIdAndStatus(publishedTopic.getId(), ContentStatus.PUBLISHED)).isTrue();

        assertThat(topicRepo.existsByIdAndStatus(draftTopic.getId(), ContentStatus.PUBLISHED)).isFalse();
        assertThat(topicRepo.existsByIdAndStatus(archivedTopic.getId(), ContentStatus.PUBLISHED)).isFalse();
    }

    @Test
    void findByIdAndStatus_returnsTopicOnlyWhenStatusMatches() {
        Topic publishedTopic = newTopic("Published Topic", ContentStatus.PUBLISHED);
        Topic draftTopic = newTopic("Draft Topic", ContentStatus.DRAFT);
        Topic archivedTopic = newTopic("Archived Topic", ContentStatus.ARCHIVED);

        Optional<Topic> found = topicRepo.findByIdAndStatus(publishedTopic.getId(), ContentStatus.PUBLISHED);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(publishedTopic.getId());

        assertThat(topicRepo.findByIdAndStatus(draftTopic.getId(), ContentStatus.PUBLISHED)).isEmpty();
        assertThat(topicRepo.findByIdAndStatus(archivedTopic.getId(), ContentStatus.PUBLISHED)).isEmpty();
    }

    @Test
    void popular_ordersByDistinctLearnerCount_tieBrokenByIdDescending() {
        String tag = "pop-" + UUID.randomUUID();

        Topic tieFirst = newTopic(tag + "-tie-first");
        Topic nobody = newTopic(tag + "-nobody");
        Topic top = newTopic(tag + "-top");
        Topic tieSecond = newTopic(tag + "-tie-second");

        learnedBy(2, tieFirst);
        learnedBy(3, top);
        learnedBy(2, tieSecond);

        Specification<Topic> spec = TopicSpecifications.forLearners(tag, null, TopicSort.POPULAR);
        Page<Topic> page = topicRepo.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Topic::getId)
                .containsExactly(top.getId(), tieSecond.getId(), tieFirst.getId(), nobody.getId());

        assertThat(page.getTotalElements()).isEqualTo(4);
    }

    @Test
    void forLearners_returnsOnlyPublished() {
        String tag = "learn-" + UUID.randomUUID();

        newTopic(tag + " Java Basic", ContentStatus.DRAFT);
        Topic published = newTopic(tag + " Java Advanced", ContentStatus.PUBLISHED);
        newTopic(tag + " Java OOP", ContentStatus.ARCHIVED);

        var result = topicRepo.findAll(TopicSpecifications.forLearners(tag, null, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(published.getId());
        assertThat(result.get(0).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
    }

    @Test
    void forLearners_popularSort_ignoresNonPublished() {
        String tag = "pop-pub-" + UUID.randomUUID();

        Topic publishedTopic = newTopic(tag + "-Published", ContentStatus.PUBLISHED);
        Topic draftTopic = newTopic(tag + "-Draft", ContentStatus.DRAFT);

        learnedBy(1, publishedTopic); // Published có 1 lượt học
        learnedBy(5, draftTopic); // Draft có hẳn 5 lượt học

        var result = topicRepo.findAll(TopicSpecifications.forLearners(tag, null, TopicSort.POPULAR));

        assertThat(result).extracting(Topic::getId).containsExactly(publishedTopic.getId());
        assertThat(result.get(0).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
    }

    @Test
    void forAdmin_nullStatus_returnsAll() {
        String tag = "admin-all-" + UUID.randomUUID();

        newTopic(tag + "-T1", ContentStatus.DRAFT);
        newTopic(tag + "-T2", ContentStatus.PUBLISHED);

        var result = topicRepo.findAll(TopicSpecifications.forAdmin(tag, null, null, null));

        assertThat(result).hasSize(2);
    }

    @Test
    void forAdmin_withStatus_filtersExactly() {
        String tag = "admin-status-" + UUID.randomUUID();

        newTopic(tag + "-T1", ContentStatus.DRAFT);
        newTopic(tag + "-T2", ContentStatus.PUBLISHED);

        var result = topicRepo.findAll(TopicSpecifications.forAdmin(tag, null, null, ContentStatus.DRAFT));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ContentStatus.DRAFT);
    }
}