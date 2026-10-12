package com.example.englishlearningplatform.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.englishlearningplatform.ai.AiChatResult;
import com.example.englishlearningplatform.ai.AiClient;
import com.example.englishlearningplatform.repository.PostgresIntegrationTestBase;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.rate-limit.login-per-minute=3",
        "app.rate-limit.register-per-hour=2",
        "app.rate-limit.chat-per-minute=2",
        "app.rate-limit.trusted-proxy=true" })
class RateLimitIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "Passw0rd!123";
    private static final String MSG_429 = "Too many requests. Please try again later";
    private static final AtomicInteger IP_SEQ = new AtomicInteger();
    private static final String BROWSER_ORIGIN = "http://localhost:5500";

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

    @MockitoBean
    AiClient aiClient; // không gọi xKiro thật
    @MockitoSpyBean
    PasswordEncoder passwordEncoder; // đếm số lần thật sự tới BCrypt

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JsonMapper jsonMapper;

    // ═══════════ Helpers ═══════════

    private static String freshIp() {
        int n = IP_SEQ.incrementAndGet();
        return "10.0." + (n / 250) + "." + (n % 250 + 1);
    }

    private MvcResult postJson(String path, Object body, String ip, String token) throws Exception {
        MockHttpServletRequestBuilder b = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body));
        if (ip != null) {
            b.header("X-Forwarded-For", ip);
        }
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(b).andReturn();
    }

    private MvcResult register(String username, String ip) throws Exception {
        return postJson("/api/auth/register", Map.of(
                "username", username, "email", username + "@example.com", "password", PASSWORD), ip, null);
    }

    private MvcResult login(String username, String password, String ip) throws Exception {
        return postJson("/api/auth/login", Map.of("username", username, "password", password), ip, null);
    }

    private MvcResult loginFromBrowser(String username, String password, String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("username", username, "password", password)))
                .header("X-Forwarded-For", ip)
                .header("Origin", BROWSER_ORIGIN))
                .andReturn();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> bodyOf(MvcResult r) throws Exception {
        return jsonMapper.readValue(r.getResponse().getContentAsString(), Map.class);
    }

    /** Đăng ký bằng IP riêng để không tiêu hao hạn mức của IP đang được test. */
    private String registerFreshUser() throws Exception {
        String username = "rl_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult r = register(username, freshIp());
        assertThat(r.getResponse().getStatus()).as("register %s", username).isBetween(200, 299);
        return username;
    }

    private Map<String, Object> loginOk(String username) throws Exception {
        MvcResult r = login(username, PASSWORD, freshIp());
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return bodyOf(r);
    }

    // ═══════════ login ═══════════

    @Test
    void login_wrongPasswordThreeTimes_fourthIs429WithRetryAfterAndStandardBody() throws Exception {
        String username = registerFreshUser();
        String ip = freshIp();

        for (int i = 0; i < 3; i++) {
            int status = login(username, "wrong-password", ip).getResponse().getStatus();
            assertThat(status).as("lần %d", i + 1).isBetween(400, 499).isNotEqualTo(429);
        }
        MvcResult blocked = login(username, "wrong-password", ip);

        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(blocked.getResponse().getHeader("Retry-After"))).isGreaterThanOrEqualTo(1L);
        Map<String, Object> body = bodyOf(blocked);
        assertThat(body.keySet()).containsExactlyInAnyOrder("message", "status", "timestamp");
        assertThat(body.get("status")).isEqualTo(429);
        assertThat(body.get("message")).isEqualTo(MSG_429);
    }

    @Test
    void login_afterBlocked_correctPasswordSameIpStill429_andNeverReachesBcrypt() throws Exception {
        String username = registerFreshUser();
        String ip = freshIp();
        clearInvocations(passwordEncoder);

        for (int i = 0; i < 3; i++) {
            login(username, "wrong-password", ip);
        }
        MvcResult blocked = login(username, PASSWORD, ip); // đúng mật khẩu

        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
        verify(passwordEncoder, times(3)).matches(any(), any()); // chỉ 3 lần sai tới BCrypt, lần bị chặn thì không
    }

    @Test
    void login_otherIpIsNotAffectedByABlockedIp() throws Exception {
        String username = registerFreshUser();
        String blockedIp = freshIp();
        for (int i = 0; i < 4; i++) {
            login(username, "wrong-password", blockedIp);
        }
        assertThat(login(username, "wrong-password", blockedIp).getResponse().getStatus()).isEqualTo(429);

        MvcResult otherIp = login(username, PASSWORD, freshIp());

        assertThat(otherIp.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void login_blocked_exposesRetryAfterHeaderToBrowsers() throws Exception {
        String username = registerFreshUser();
        String ip = freshIp();

        for (int i = 0; i < 3; i++) {
            int status = loginFromBrowser(username, "wrong-password", ip).getResponse().getStatus();
            assertThat(status).as("lần %d", i + 1).isBetween(400, 499).isNotEqualTo(429);
        }
        MvcResult blocked = loginFromBrowser(username, "wrong-password", ip);

        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
        assertThat(blocked.getResponse().getHeader("Retry-After")).isNotNull();
        assertThat(blocked.getResponse().getHeader("Access-Control-Expose-Headers"))
                .as("trình duyệt chỉ đọc được Retry-After nếu nó nằm trong Expose-Headers")
                .contains("Retry-After");
    }

    // ═══════════ register ═══════════

    @Test
    void register_thirdAttemptFromSameIp_is429() throws Exception {
        String ip = freshIp();

        for (int i = 0; i < 2; i++) {
            MvcResult r = register("rg_" + UUID.randomUUID().toString().substring(0, 8), ip);
            assertThat(r.getResponse().getStatus()).as("lần %d", i + 1).isBetween(200, 299);
        }
        MvcResult third = register("rg_" + UUID.randomUUID().toString().substring(0, 8), ip);

        assertThat(third.getResponse().getStatus()).isEqualTo(429);
    }

    // ═══════════ chat, quan trọng nhất ═══════════

    @Test
    void sendMessage_thirdRequest_is429_andMessageIsNotSaved() throws Exception {
        when(aiClient.chat(anyList())).thenReturn(new AiChatResult("AI reply", null, null));
        String username = registerFreshUser();
        String token = (String) loginOk(username).get("accessToken");

        MvcResult created = postJson("/api/conversations", Map.of(), null, token);
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        Object conversationId = bodyOf(created).get("id");
        String messagesPath = "/api/conversations/" + conversationId + "/messages";

        for (int i = 0; i < 2; i++) {
            MvcResult r = postJson(messagesPath, Map.of("content", "hello " + i), null, token);
            assertThat(r.getResponse().getStatus()).as("tin %d", i + 1).isEqualTo(200);
        }
        int before = countMessages(messagesPath, token);
        assertThat(before).isEqualTo(4); // 2 tin user + 2 tin AI

        MvcResult blocked = postJson(messagesPath, Map.of("content", "hello blocked"), null, token);

        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
        assertThat(countMessages(messagesPath, token)).isEqualTo(before); // tin bị chặn KHÔNG được lưu
    }

    private int countMessages(String messagesPath, String token) throws Exception {
        MvcResult r = mockMvc.perform(get(messagesPath).header("Authorization", "Bearer " + token)).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return jsonMapper.readValue(r.getResponse().getContentAsString(), List.class).size();
    }

    // ═══════════ refresh không bị giới hạn ═══════════

    @Test
    void refresh_isNotRateLimited() throws Exception {
        String username = registerFreshUser();
        String refreshToken = (String) loginOk(username).get("refreshToken");
        String ip = freshIp(); // cùng một IP cho cả 20 lần

        for (int i = 0; i < 20; i++) {
            MvcResult r = postJson("/api/auth/refresh", Map.of("refreshToken", refreshToken), ip, null);
            assertThat(r.getResponse().getStatus()).as("lần %d", i + 1).isEqualTo(200);
        }
    }
}