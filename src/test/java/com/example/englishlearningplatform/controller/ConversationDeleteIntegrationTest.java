package com.example.englishlearningplatform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.englishlearningplatform.ai.AiChatResult;
import com.example.englishlearningplatform.ai.AiClient;
import com.example.englishlearningplatform.ratelimit.RateLimitDecision;
import com.example.englishlearningplatform.ratelimit.RateLimiter;
import com.example.englishlearningplatform.repository.MessageRepository;
import com.example.englishlearningplatform.repository.PostgresIntegrationTestBase;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationDeleteIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "Passw0rd!123";

    @Autowired
    MessageRepository messageRepository;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("topics", "topicDetails", "flashcardsByTopic");
        }

        @Bean
        @Primary
        RateLimiter alwaysAllowRateLimiter() {
            return (key, limit, window) -> RateLimitDecision.allow();
        }
    }

    @MockitoBean
    AiClient aiClient;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JsonMapper jsonMapper;

    // ═══════════ Helpers ═══════════

    @SuppressWarnings("unchecked")
    private Map<String, Object> bodyOf(MvcResult r) throws Exception {
        return jsonMapper.readValue(r.getResponse().getContentAsString(), Map.class);
    }

    private String registerAndLogin() throws Exception {
        String username = "cd_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                        "username", username, "email", username + "@example.com", "password", PASSWORD))))
                .andReturn();
        assertThat(reg.getResponse().getStatus()).as("register").isBetween(200, 299);

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("username", username, "password", PASSWORD))))
                .andReturn();
        assertThat(login.getResponse().getStatus()).as("login").isEqualTo(200);
        return (String) bodyOf(login).get("accessToken");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    /** A tạo conversation và gửi 2 tin (-> 4 message: 2 user + 2 AI). Trả về id. */
    private Object createConversationWithTwoExchanges(String token) throws Exception {
        when(aiClient.chat(anyList())).thenReturn(new AiChatResult("AI reply", null, null));

        MvcResult created = mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
                .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        Object id = bodyOf(created).get("id");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/conversations/" + id + "/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(Map.of("content", "hello " + i)))
                    .header("Authorization", bearer(token)))
                    .andExpect(status().isOk());
        }
        return id;
    }

    // ═══════════ Tests ═══════════

    @Test
    void delete_byNonOwner_is404_andOwnerStillSeesAllMessages() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        Object id = createConversationWithTwoExchanges(tokenA);

        mockMvc.perform(delete("/api/conversations/" + id).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/conversations/" + id + "/messages").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void delete_byOwner_is204_thenConversationAndMessagesAre404() throws Exception {
        String token = registerAndLogin();
        Object id = createConversationWithTwoExchanges(token);

        mockMvc.perform(delete("/api/conversations/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/conversations/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/conversations/" + id + "/messages").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_byOwner_is204_removesExactlyItsMessagesFromDb_thenConversationIs404() throws Exception {
        String token = registerAndLogin();
        Object id = createConversationWithTwoExchanges(token);
        long before = messageRepository.count();

        mockMvc.perform(delete("/api/conversations/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(before - messageRepository.count())
                .as("xoá conversation phải xoá đúng 4 message của nó (2 user + 2 AI) khỏi DB")
                .isEqualTo(4);
        mockMvc.perform(get("/api/conversations/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/conversations/" + id + "/messages").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }
}