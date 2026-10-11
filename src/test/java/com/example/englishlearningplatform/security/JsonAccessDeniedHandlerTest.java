package com.example.englishlearningplatform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.json.JsonMapper;

class JsonAccessDeniedHandlerTest {

    private static final String FIXED_MESSAGE = "You do not have permission to access this resource";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonAccessDeniedHandler handler = new JsonAccessDeniedHandler(jsonMapper);

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("POST", "/api/admin/topics");
        response = new MockHttpServletResponse();

        logger = (Logger) LoggerFactory.getLogger(JsonAccessDeniedHandler.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
    }

    // ---------- helpers ----------

    private void invoke(AccessDeniedException ex) throws Exception {
        handler.handle(request, response, ex);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readBody() throws UnsupportedEncodingException {
        String json = response.getContentAsString(StandardCharsets.UTF_8);
        return jsonMapper.readValue(json, Map.class);
    }

    private String allLogText() {
        StringBuilder sb = new StringBuilder();
        for (ILoggingEvent event : appender.list) {
            sb.append('\n').append(event.getFormattedMessage());
            if (event.getThrowableProxy() != null) {
                sb.append(' ').append(event.getThrowableProxy().getClassName())
                        .append(' ').append(event.getThrowableProxy().getMessage());
            }
        }
        return sb.toString();
    }

    // ---------- tests ----------

    @Test
    void handle_returns403() throws Exception {
        invoke(new AccessDeniedException("x"));

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void handle_setsJsonContentTypeAndUtf8() throws Exception {
        invoke(new AccessDeniedException("x"));

        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentType()).containsIgnoringCase("charset=UTF-8");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
    }

    @Test
    void handle_bodyHasExactlyThreeFields() throws Exception {
        invoke(new AccessDeniedException("x"));

        Map<String, Object> body = readBody();

        assertThat(body.keySet()).containsExactlyInAnyOrder("message", "status", "timestamp");
        assertThat(body.get("status")).isEqualTo(403);
        assertThat(body.get("status")).isEqualTo(response.getStatus());
    }

    @Test
    void handle_messageIsTheFixedConstant() throws Exception {
        invoke(new AccessDeniedException("x"));

        assertThat(readBody().get("message")).isEqualTo(FIXED_MESSAGE);
    }

    @Test
    void handle_timestampIsRecentInstant() throws Exception {
        invoke(new AccessDeniedException("x"));

        Instant timestamp = Instant.parse((String) readBody().get("timestamp"));

        assertThat(Duration.between(timestamp, Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void handle_doesNotLeakExceptionMessage() throws Exception {
        invoke(new AccessDeniedException("SECRET_DETAIL"));

        assertThat(response.getContentAsString()).doesNotContain("SECRET_DETAIL");
        assertThat(response.getContentAsString()).doesNotContain("AccessDeniedException");
    }

    @Test
    void handle_doesNotLogSecretsFromHeaderOrQuery() throws Exception {
        request.addHeader("Authorization", "Bearer SECRET_TOKEN");
        request.setQueryString("token=SECRET_QUERY");
        request.addParameter("username", "SECRET_USER");

        invoke(new AccessDeniedException("SECRET_DETAIL"));

        assertThat(appender.list).isNotEmpty();
        String logText = allLogText();
        assertThat(logText).doesNotContain("SECRET_TOKEN", "SECRET_QUERY", "SECRET_USER", "SECRET_DETAIL");
        assertThat(logText).contains("POST", "/api/admin/topics");
    }

    @Test
    void handle_logsAtWarnLevel() throws Exception {
        invoke(new AccessDeniedException("x"));

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.WARN);
    }
}