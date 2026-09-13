package com.example.englishlearningplatform.event;

public class TopicChangedEvent {

    // null = chỉ cần evict cache "topics" (list) — case tạo mới Topic,
    // chưa từng có cache detail nào cho id này để evict.
    // có giá trị = evict thêm cache "topicDetails" đúng key này — case
    // update/delete Topic đã tồn tại.
    private final Long topicId;

    public TopicChangedEvent(Long topicId) {
        this.topicId = topicId;
    }

    public Long getTopicId() {
        return topicId;
    }
}