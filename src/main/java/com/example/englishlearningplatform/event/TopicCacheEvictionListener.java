package com.example.englishlearningplatform.event;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TopicCacheEvictionListener {

    private final CacheManager cacheManager;

    public TopicCacheEvictionListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * phase = AFTER_COMMIT (mặc định của @TransactionalEventListener nếu không
     * ghi rõ, nhưng ghi tường minh cho dễ đọc): listener này CHỈ chạy sau khi
     * transaction bao quanh createTopic/updateTopic/deleteTopic đã commit
     * thành công thật sự — không còn race condition evict-trước-commit như
     * cách dùng @CacheEvict trực tiếp trước đây.
     *
     * Nếu transaction rollback (VD: DataIntegrityViolationException ném ra
     * giữa chừng), event này sẽ KHÔNG bao giờ được deliver — đúng ý muốn,
     * vì không có gì thay đổi thật trong DB thì không cần evict.
     *
     * fallbackExecution mặc định = false: nếu publishEvent() được gọi mà
     * KHÔNG nằm trong transaction nào (không nên xảy ra vì cả 3 method
     * createTopic/updateTopic/deleteTopic đều có @Transactional), event sẽ bị
     * bỏ qua thay vì chạy ngay — chấp nhận được vì đây không phải use case
     * thực tế của bạn hiện tại.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTopicChanged(TopicChangedEvent event) {
        Cache topicsCache = cacheManager.getCache("topics");
        if (topicsCache != null) {
            topicsCache.clear(); // tương đương allEntries = true trước đây
        }

        if (event.getTopicId() != null) {
            Cache topicDetailsCache = cacheManager.getCache("topicDetails");
            if (topicDetailsCache != null) {
                topicDetailsCache.evict(event.getTopicId());
            }
        }
    }
}