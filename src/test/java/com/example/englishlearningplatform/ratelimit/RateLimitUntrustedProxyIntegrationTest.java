package com.example.englishlearningplatform.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.englishlearningplatform.repository.PostgresIntegrationTestBase;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.rate-limit.login-per-minute=3",
        "app.rate-limit.register-per-hour=100",
        "app.rate-limit.chat-per-minute=100",
        "app.rate-limit.trusted-proxy=false" })
class RateLimitUntrustedProxyIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "Passw0rd!123";

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("topics", "topicDetails", "flashcardsByTopic");
        }

        @Bean
        @Primary
        RateLimiter testRateLimiter() {
            return new InMemoryRateLimiter(Clock.systemUTC());
        }
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JsonMapper jsonMapper;

    private MvcResult login(String username, String password, String forwardedFor) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", forwardedFor)
                .content(jsonMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andReturn();
    }

    @Test
    void untrustedProxy_rotatingForwardedForHeader_doesNotEvadeTheLimit() throws Exception {
        String username = "up_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                        "username", username, "email", username + "@example.com", "password", PASSWORD))))
                .andReturn();
        assertThat(registered.getResponse().getStatus()).isBetween(200, 299);

        String[] spoofed = { "203.0.113.1", "203.0.113.2", "203.0.113.3", "203.0.113.4" };
        for (int i = 0; i < 3; i++) {
            int status = login(username, "wrong-password", spoofed[i]).getResponse().getStatus();
            assertThat(status).as("lần %d", i + 1).isBetween(400, 499).isNotEqualTo(429);
        }

        MvcResult fourth = login(username, "wrong-password", spoofed[3]);

        assertThat(fourth.getResponse().getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(fourth.getResponse().getHeader("Retry-After"))).isGreaterThanOrEqualTo(1L);
    }
}