package com.example.englishlearningplatform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.json.JsonMapper;

class JsonAuthenticationEntryPointTest {

    private static final String FIXED_MESSAGE = "You need to login to access this resource";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(jsonMapper);

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/api/admin/topics");
        response = new MockHttpServletResponse();

        logger = (Logger) LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);
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

    private void invoke(AuthenticationException ex) throws Exception {
        entryPoint.commence(request, response, ex);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readBody() throws UnsupportedEncodingException {
        String json = response.getContentAsString(StandardCharsets.UTF_8);
        return jsonMapper.readValue(json, Map.class);
    }

    // Nếu code log kiểu log.debug("...", ex) thì message của exception nằm trong
    // throwableProxy,
    // không nằm trong getFormattedMessage()
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
    void commence_returns401() throws Exception {
        invoke(new BadCredentialsException("x"));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void commence_setsJsonContentTypeAndUtf8() throws Exception {
        invoke(new BadCredentialsException("x"));

        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentType()).containsIgnoringCase("charset=UTF-8");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
    }

    @Test
    void commence_bodyHasExactlyThreeFields() throws Exception {
        invoke(new BadCredentialsException("x"));

        Map<String, Object> body = readBody();

        assertThat(body.keySet()).containsExactlyInAnyOrder("message", "status", "timestamp");
        assertThat(body.get("status")).isEqualTo(401);
        assertThat(body.get("status")).isEqualTo(response.getStatus());
    }

    @Test
    void commence_messageIsTheFixedConstant() throws Exception {
        invoke(new BadCredentialsException("x"));

        assertThat(readBody().get("message")).isEqualTo(FIXED_MESSAGE);
    }

    @Test
    void commence_timestampIsRecentInstant() throws Exception {
        invoke(new BadCredentialsException("x"));

        Instant timestamp = Instant.parse((String) readBody().get("timestamp"));

        assertThat(Duration.between(timestamp, Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void commence_doesNotLeakExceptionMessage() throws Exception {
        invoke(new BadCredentialsException("SECRET_DETAIL"));

        assertThat(response.getContentAsString()).doesNotContain("SECRET_DETAIL");
        assertThat(response.getContentAsString()).doesNotContain("BadCredentialsException");
    }

    @Test
    void commence_doesNotLogSecretsFromHeaderOrQuery() throws Exception {
        request.addHeader("Authorization", "Bearer SECRET_TOKEN");
        request.setQueryString("token=SECRET_QUERY");
        request.addParameter("username", "SECRET_USER");

        invoke(new BadCredentialsException("SECRET_DETAIL"));

        assertThat(appender.list).isNotEmpty();
        String logText = allLogText();
        assertThat(logText).doesNotContain("SECRET_TOKEN", "SECRET_QUERY", "SECRET_USER", "SECRET_DETAIL");
        assertThat(logText).contains("GET", "/api/admin/topics");
    }

    @Test
    void commence_logsOnlyAtDebugLevel() throws Exception {
        invoke(new BadCredentialsException("x"));

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list).allMatch(event -> event.getLevel() == Level.DEBUG);
    }

    @Test
    void commence_producesTheSameResponseForAnyAuthenticationException() throws Exception {
        List<AuthenticationException> exceptions = List.of(
                new BadCredentialsException("a"),
                new InsufficientAuthenticationException("b"),
                new AccountExpiredException("c"));

        for (AuthenticationException ex : exceptions) {
            response = new MockHttpServletResponse();
            invoke(ex);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(readBody().get("message")).isEqualTo(FIXED_MESSAGE);
        }
    }
}