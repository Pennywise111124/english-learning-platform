package com.example.englishlearningplatform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.example.englishlearningplatform.entity.Role;
import com.example.englishlearningplatform.ratelimit.RateLimitDecision;
import com.example.englishlearningplatform.ratelimit.RateLimiter;
import com.example.englishlearningplatform.repository.PostgresIntegrationTestBase;
import com.example.englishlearningplatform.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityMatrixIntegrationTest extends PostgresIntegrationTestBase {

    private static final String MSG_401 = "You need to login to access this resource";
    private static final String MSG_403 = "You do not have permission to access this resource";
    private static final String PASSWORD = "Passw0rd!123";

    @TestConfiguration
    static class TestCacheConfig {
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

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JsonMapper jsonMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    private static String userToken;
    private static String adminToken;

    // ═══════════ Danh sách route ═══════════

    static Stream<Arguments> adminRoutes() {
        return Stream.of(
                Arguments.of("GET", "/api/admin/topics"),
                Arguments.of("GET", "/api/admin/topics/999999"),
                Arguments.of("POST", "/api/admin/topics"),
                Arguments.of("PUT", "/api/admin/topics/999999"),
                Arguments.of("DELETE", "/api/admin/topics/999999"),
                Arguments.of("PATCH", "/api/admin/topics/999999/status"),
                Arguments.of("GET", "/api/admin/topics/999999/dictation"),
                Arguments.of("GET", "/api/admin/quizzes/999999/questions"),
                Arguments.of("DELETE", "/api/admin/questions/999999"),
                Arguments.of("PATCH", "/api/admin/quizzes/999999/status"),
                Arguments.of("PATCH", "/api/admin/dictation/999999/status"),
                Arguments.of("POST", "/api/admin/dictation/999999/audio"));
    }

    static Stream<Arguments> authenticatedRoutes() {
        return Stream.of(
                Arguments.of("GET", "/api/users/me"),
                Arguments.of("GET", "/api/users/me/progress"),
                Arguments.of("GET", "/api/vocabulary/today"),
                Arguments.of("GET", "/api/conversations"),
                Arguments.of("DELETE", "/api/conversations/999999"),
                Arguments.of("GET", "/api/quizzes/999999"),
                Arguments.of("GET", "/api/dictation/lessons"),
                Arguments.of("GET", "/api/topics/999999/dictation"),
                Arguments.of("GET", "/api/users/me/stats"),
                Arguments.of("PATCH", "/api/users/me/timezone"),
                Arguments.of("GET", "/api/leaderboard/weekly"),
                Arguments.of("GET", "/api/users/me/achievements"),
                Arguments.of("POST", "/api/users/me/achievements/XP_100/dismiss"),
                Arguments.of("PATCH", "/api/users/me/leaderboard-visibility"));
    }

    static Stream<Arguments> publicRoutes() {
        return Stream.of(
                Arguments.of("GET", "/api/topics"),
                Arguments.of("GET", "/api/topics/999999"),
                Arguments.of("GET", "/api/topics/999999/flashcards"),
                Arguments.of("GET", "/api/topics/999999/quizzes"),
                Arguments.of("GET", "/v3/api-docs"),
                Arguments.of("GET", "/uploads/not-exist.jpg"));
    }

    // ═══════════ Setup ═══════════

    @BeforeAll
    void createUsers() throws Exception {
        String user = uniqueUsername("user");
        register(user);
        userToken = login(user);

        String admin = uniqueUsername("admin");
        register(admin);
        promoteToAdmin(admin);
        adminToken = login(admin);
    }

    // ═══════════ Helpers ═══════════

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void register(String username) throws Exception {
        String json = jsonMapper.writeValueAsString(Map.of(
                "username", username,
                "email", username + "@example.com",
                "password", PASSWORD));
        MvcResult r = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andReturn();
        assertThat(r.getResponse().getStatus())
                .as("register %s: %s", username, r.getResponse().getContentAsString())
                .isBetween(200, 299);
    }

    private String login(String username) throws Exception {
        String json = jsonMapper.writeValueAsString(Map.of(
                "username", username,
                "password", PASSWORD));
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andReturn();
        assertThat(r.getResponse().getStatus())
                .as("login %s: %s", username, r.getResponse().getContentAsString())
                .isEqualTo(200);
        return (String) body(r).get("accessToken");
    }

    private void promoteToAdmin(String username) {
        var user = userRepository.findByUsername(username).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }

    private MockHttpServletRequestBuilder req(String method, String path, String token) {
        MockHttpServletRequestBuilder b = request(HttpMethod.valueOf(method), path);
        if (!method.equals("GET") && !method.equals("DELETE")) {
            b.contentType(MediaType.APPLICATION_JSON).content("{"); // JSON hỏng: luôn 400, không ghi dữ liệu
        }
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return b;
    }

    private MvcResult call(String method, String path, String token) throws Exception {
        return mockMvc.perform(req(method, path, token)).andReturn();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(MvcResult r) throws Exception {
        return jsonMapper.readValue(r.getResponse().getContentAsString(), Map.class);
    }

    private void assertErrorBody(MvcResult r, int status, String message) throws Exception {
        assertThat(r.getResponse().getStatus()).isEqualTo(status);
        Map<String, Object> b = body(r);
        assertThat(b.keySet()).containsExactlyInAnyOrder("message", "status", "timestamp");
        assertThat(b.get("status")).isEqualTo(status);
        assertThat(b.get("message")).isEqualTo(message);
        Instant.parse((String) b.get("timestamp")); // parse được = đúng định dạng ISO
    }

    private void assertPassesSecurity(MvcResult r) {
        assertThat(r.getResponse().getStatus()).isNotIn(401, 403);
    }

    // ═══════════ Tests ═══════════

    @ParameterizedTest(name = "{0} {1}: anonymous -> 401")
    @MethodSource("adminRoutes")
    void adminRoutes_anonymous_get401WithJsonBody(String method, String path) throws Exception {
        assertErrorBody(call(method, path, null), 401, MSG_401);
    }

    @ParameterizedTest(name = "{0} {1}: USER -> 403")
    @MethodSource("adminRoutes")
    void adminRoutes_user_get403WithJsonBody(String method, String path) throws Exception {
        assertErrorBody(call(method, path, userToken), 403, MSG_403);
    }

    @ParameterizedTest(name = "{0} {1}: ADMIN passes security")
    @MethodSource("adminRoutes")
    void adminRoutes_admin_passSecurity(String method, String path) throws Exception {
        assertPassesSecurity(call(method, path, adminToken));
    }

    @ParameterizedTest(name = "{0} {1}: anonymous -> 401")
    @MethodSource("authenticatedRoutes")
    void authenticatedRoutes_anonymous_get401(String method, String path) throws Exception {
        assertErrorBody(call(method, path, null), 401, MSG_401);
    }

    @ParameterizedTest(name = "{0} {1}: USER passes security")
    @MethodSource("authenticatedRoutes")
    void authenticatedRoutes_user_passSecurity(String method, String path) throws Exception {
        assertPassesSecurity(call(method, path, userToken));
    }

    @ParameterizedTest(name = "{0} {1}: anonymous passes security")
    @MethodSource("publicRoutes")
    void publicRoutes_anonymous_passSecurity(String method, String path) throws Exception {
        assertPassesSecurity(call(method, path, null));
    }

    @Test
    void garbageToken_get401WithJsonBody() throws Exception {
        assertErrorBody(call("GET", "/api/users/me", "not.a.jwt"), 401, MSG_401);
    }

    @Test
    void tokenWithTamperedSignature_get401() throws Exception {
        int sigStart = userToken.lastIndexOf('.') + 1;
        char original = userToken.charAt(sigStart);
        char replacement = original == 'A' ? 'B' : 'A';
        String tampered = userToken.substring(0, sigStart) + replacement + userToken.substring(sigStart + 1);

        assertErrorBody(call("GET", "/api/users/me", tampered), 401, MSG_401);
    }

    @Test
    void everyRegisteredAdminEndpoint_isProtected() throws Exception {
        List<String> failures = new ArrayList<>();
        int checked = 0;

        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            if (info.getPathPatternsCondition() == null)
                continue;
            Set<String> patterns = info.getPathPatternsCondition().getPatternValues();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();

            for (String pattern : patterns) {
                if (!pattern.startsWith("/api/admin/"))
                    continue;
                String path = pattern.replaceAll("\\{[^}]+}", "999999");
                for (RequestMethod m : methods.isEmpty() ? Set.of(RequestMethod.GET) : methods) {
                    checked++;
                    int anon = call(m.name(), path, null).getResponse().getStatus();
                    int user = call(m.name(), path, userToken).getResponse().getStatus();
                    if (anon != 401)
                        failures.add("anon " + m + " " + path + " -> " + anon);
                    if (user != 403)
                        failures.add("user " + m + " " + path + " -> " + user);
                }
            }
        }

        assertThat(checked).isGreaterThanOrEqualTo((int) adminRoutes().count()); // chống quét ra 0 route
        assertThat(failures).isEmpty();
    }
}