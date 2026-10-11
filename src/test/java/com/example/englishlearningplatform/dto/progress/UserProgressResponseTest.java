package com.example.englishlearningplatform.dto.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.ProgressStatus;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.entity.UserProgress;

class UserProgressResponseTest {

    @ParameterizedTest
    @EnumSource(ContentStatus.class)
    void from_topicAvailableOnlyWhenPublished(ContentStatus topicStatus) {
        Topic topic = new Topic();
        topic.setId(7L);
        topic.setTitle("Travel");
        topic.setStatus(topicStatus);

        Instant now = Instant.parse("2026-10-09T00:00:00Z");
        UserProgress p = new UserProgress();
        p.setTopic(topic);
        p.setStatus(ProgressStatus.IN_PROGRESS);
        p.setProgressPercent(60);
        p.setUpdatedAt(now);

        UserProgressResponse r = UserProgressResponse.from(p);

        assertEquals(topicStatus == ContentStatus.PUBLISHED, r.isTopicAvailable());
        assertEquals(7L, r.getTopicId());
        assertEquals("Travel", r.getTopicTitle());
        assertEquals(ProgressStatus.IN_PROGRESS, r.getStatus());
        assertEquals(60, r.getProgressPercent());
        assertEquals(now, r.getUpdatedAt());
    }
}