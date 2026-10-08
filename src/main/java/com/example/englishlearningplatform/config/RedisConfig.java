package com.example.englishlearningplatform.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

        @Value("${app.cache.topic-list-ttl-minutes:10}")
        private long topicListTtl;

        @Value("${app.cache.topic-detail-ttl-minutes:30}")
        private long topicDetailTtl;

        @Value("${app.cache.flashcard-list-ttl-minutes:30}")
        private long flashcardListTtl;

        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

                RedisCacheConfiguration defaulConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .disableCachingNullValues()
                                .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(jsonSerializer))
                                .entryTtl(Duration.ofHours(1));

                RedisCacheConfiguration listCacheConfig = defaulConfig.entryTtl(Duration.ofMinutes(topicListTtl));
                RedisCacheConfiguration detailCacheConfig = defaulConfig.entryTtl(Duration.ofMinutes(topicDetailTtl));

                RedisCacheConfiguration flashcardListCacheConfig = defaulConfig
                                .entryTtl(Duration.ofMinutes(flashcardListTtl));

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