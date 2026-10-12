package com.example.englishlearningplatform.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.example.englishlearningplatform.config.RateLimitProperties;
import com.example.englishlearningplatform.exception.RateLimitExceededException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    private static final String IP = "1.2.3.4";
    private static final String USER = "alice";

    @Mock
    RateLimiter rateLimiter;

    private RateLimitService service;

    private final Logger serviceLogger = (Logger) LoggerFactory.getLogger(RateLimitService.class);
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        service = new RateLimitService(rateLimiter, new RateLimitProperties(7, 3, 13, false));

        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logAppender);
    }

    static Stream<Arguments> allChecks() {
        return Stream.of(
                Arguments.of("login", (Consumer<RateLimitService>) s -> s.checkLogin(IP)),
                Arguments.of("register", (Consumer<RateLimitService>) s -> s.checkRegister(IP)),
                Arguments.of("chat", (Consumer<RateLimitService>) s -> s.checkChat(USER)));
    }

    private void givenAllowed() {
        when(rateLimiter.tryAcquire(any(), anyInt(), any())).thenReturn(RateLimitDecision.allow());
    }

    // ---------- key, limit, window lấy đúng từ props ----------

    @Test
    void checkLogin_usesLoginKeyLimitFromPropsAndOneMinuteWindow() {
        givenAllowed();

        service.checkLogin(IP);

        verify(rateLimiter).tryAcquire(eq("rl:login:1.2.3.4"), eq(7), eq(Duration.ofMinutes(1)));
    }

    @Test
    void checkRegister_usesRegisterKeyLimitFromPropsAndOneHourWindow() {
        givenAllowed();

        service.checkRegister(IP);

        verify(rateLimiter).tryAcquire(eq("rl:register:1.2.3.4"), eq(3), eq(Duration.ofHours(1)));
    }

    @Test
    void checkChat_usesChatKeyLimitFromPropsAndOneMinuteWindow() {
        givenAllowed();

        service.checkChat(USER);

        verify(rateLimiter).tryAcquire(eq("rl:chat:alice"), eq(13), eq(Duration.ofMinutes(1)));
    }

    // ---------- kết quả của limiter ----------

    @ParameterizedTest(name = "{0}: deny(42) -> ném với retryAfter 42")
    @MethodSource("allChecks")
    void denied_throwsRateLimitExceededWithItsRetryAfter(String name, Consumer<RateLimitService> check) {
        when(rateLimiter.tryAcquire(any(), anyInt(), any())).thenReturn(RateLimitDecision.deny(42));

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class, () -> check.accept(service));

        assertEquals(42L, ex.getRetryAfterSeconds());
    }

    @ParameterizedTest(name = "{0}: allow -> không ném")
    @MethodSource("allChecks")
    void allowed_doesNotThrow(String name, Consumer<RateLimitService> check) {
        givenAllowed();

        assertDoesNotThrow(() -> check.accept(service));
    }

    // ---------- fail open, cả ba check ----------

    @ParameterizedTest(name = "{0}: limiter lỗi -> fail open")
    @MethodSource("allChecks")
    void limiterThrowing_failsOpen(String name, Consumer<RateLimitService> check) {
        when(rateLimiter.tryAcquire(any(), anyInt(), any())).thenThrow(new IllegalStateException("redis down"));

        assertDoesNotThrow(() -> check.accept(service));
    }

    // ---------- tiền tố khoá tách bạch ----------

    @Test
    void sameIdentifier_usesDifferentKeysForLoginRegisterAndChat() {
        givenAllowed();

        service.checkLogin("x");
        service.checkRegister("x");
        service.checkChat("x");

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter, times(3)).tryAcquire(keys.capture(), anyInt(), any());
        assertThat(keys.getAllValues()).containsExactly("rl:login:x", "rl:register:x", "rl:chat:x");
    }

    // ---------- log của fail open không lộ thông tin ----------

    @Test
    void failOpen_logsWarnWithExceptionClassOnly_noKeyIpUsernameOrMessage() {
        when(rateLimiter.tryAcquire(any(), anyInt(), any())).thenThrow(new IllegalStateException("redis down"));

        service.checkLogin("9.9.9.9");
        service.checkChat("secret_user");

        assertThat(logAppender.list).hasSize(2).allSatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.WARN);
            assertThat(e.getFormattedMessage())
                    .contains("IllegalStateException")
                    .doesNotContain("9.9.9.9")
                    .doesNotContain("secret_user")
                    .doesNotContain("rl:")
                    .doesNotContain("redis down");
            assertThat(e.getThrowableProxy()).isNull();
        });
    }
}