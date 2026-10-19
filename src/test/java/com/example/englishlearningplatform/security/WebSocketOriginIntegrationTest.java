package com.example.englishlearningplatform.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.englishlearningplatform.repository.PostgresIntegrationTestBase;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cors.allowed-origin-patterns=https://good.example")
class WebSocketOriginIntegrationTest extends PostgresIntegrationTestBase {

    @TestConfiguration
    static class TestCacheConfig {
        @Bean
        @Primary
        CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("topics", "topicDetails", "flashcardsByTopic");
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void sockJsInfo_fromConfiguredOrigin_isAllowed() throws Exception {
        mockMvc.perform(get("/ws/chat/info").header("Origin", "https://good.example"))
                .andExpect(status().isOk());
    }

    @Test
    void sockJsInfo_fromOtherOrigin_isRejected() throws Exception {
        mockMvc.perform(get("/ws/chat/info").header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());
    }
}
