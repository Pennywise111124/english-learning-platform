package com.example.englishlearningplatform;

import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootApplication
public class EnglishLearningPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishLearningPlatformApplication.class, args);
    }

    // @Bean
    // public CommandLineRunner testRedis(RedisTemplate<String, Object>
    // redisTemplate) {
    // return args -> {
    // System.out.println("====== START TESTING REDIS ======");

    // // Test String
    // redisTemplate.opsForValue().set("test:key", "Hello Redis Docker!");
    // Object val = redisTemplate.opsForValue().get("test:key");
    // System.out.println("Result: " + val);

    // System.out.println("====== REDIS TEST SUCCESSFUL ======");
    // };
    // }

}