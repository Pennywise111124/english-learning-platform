package com.example.englishlearningplatform.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * TODO — quyết định serializer (xem ghi chú Jackson 2 vs Jackson 3 ở trên).
     * Gợi ý hướng đơn giản nhất để tránh bug default typing: dùng
     * GenericJackson3JsonRedisSerializer nhưng KHÔNG activate default typing
     * (constructor mặc định của nó, không gọi thêm activateDefaultTyping).
     * Tự thử nghiệm set/get trước khi tin tưởng.
     */
    @Value("${app.cache.topic-list-ttl-minutes:10}")
    private long topicListTtl;

    @Value("${app.cache.topic-detail-ttl-minutes:30}")
    private long topicDetailTtl;

    @Value("${app.cache.flashcard-list-ttl-minutes:30}")
    private long flashcardListTtl;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // TODO:
        // 1. Tạo RedisCacheConfiguration.defaultCacheConfig()
        // .entryTtl(Duration.ofMinutes(...)) // TTL là lưới an toàn dự
        // // phòng, không thay thế
        // // invalidate chủ động
        // .disableCachingNullValues()
        // .serializeKeysWith(RedisSerializationContext.SerializationPair
        // .fromSerializer(new StringRedisSerializer()))
        // .serializeValuesWith(... serializer đã chọn ở trên ...)
        //
        // 2. return RedisCacheManager.builder(connectionFactory)
        // .cacheDefaults(cacheConfig)
        // // TODO: cân nhắc dùng .withCacheConfiguration("topicsFirstPage", ...)
        // // riêng nếu muốn TTL khác giữa cache list và cache detail
        // .build();
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        RedisCacheConfiguration defaulConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .entryTtl(Duration.ofHours(1));

        RedisCacheConfiguration listCacheConfig = defaulConfig.entryTtl(Duration.ofMinutes(topicListTtl));
        RedisCacheConfiguration detailCacheConfig = defaulConfig.entryTtl(Duration.ofMinutes(topicDetailTtl));

        RedisCacheConfiguration flashcardListCacheConfig = defaulConfig.entryTtl(Duration.ofMinutes(flashcardListTtl));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaulConfig)
                .withCacheConfiguration("topics", listCacheConfig)
                .withCacheConfiguration("topicDetails", detailCacheConfig)
                .withCacheConfiguration("flashcardsByTopic", flashcardListCacheConfig)
                .build();
    }

    // @Bean
    // public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory
    // connectionFactory) {
    // RedisTemplate<String, Object> template = new RedisTemplate<>();
    // template.setConnectionFactory(connectionFactory);

    // template.setKeySerializer(new StringRedisSerializer());
    // template.setHashKeySerializer(new StringRedisSerializer());

    // // Dùng JSON Serializer chuẩn
    // RedisSerializer<Object> jsonSerializer = RedisSerializer.json();
    // template.setValueSerializer(jsonSerializer);
    // template.setHashValueSerializer(jsonSerializer);

    // template.afterPropertiesSet();
    // return template;
    // }
}