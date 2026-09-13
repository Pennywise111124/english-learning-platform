package com.example.englishlearningplatform.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-32-bytes-long-for-hmac-sha";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 3_600_000L;
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 604_800_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpirationMs", ACCESS_TOKEN_EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpirationMs", REFRESH_TOKEN_EXPIRATION_MS);
    }

    @Test
    void generateAccessToken_thenExtractUsername_shouldReturnOriginalUsername() {
        String token = jwtUtil.generateAccessToken("testuser", "USER");
        String extracted = jwtUtil.extractUsername(token);

        assertEquals("testuser", extracted);
    }

    @Test
    void generateAccessToken_shouldBeValid() {
        String token = jwtUtil.generateAccessToken("testuser", "USER");

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void generateRefreshToken_shouldBeValid() {
        String token = jwtUtil.generateRefreshToken("testuser");

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_whenTokenIsExpired_shouldReturnFalse() {
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpirationMs", -10_000L);

        String expiredToken = jwtUtil.generateAccessToken("testuser", "USER");

        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    void validateToken_whenSignatureIsTampered_shouldReturnFalse() {
        String validToken = jwtUtil.generateAccessToken("testuser", "USER");
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

        assertFalse(jwtUtil.validateToken(tamperedToken));
    }

    @Test
    void validateToken_whenTokenIsCompletelyMalformed_shouldReturnFalse() {
        assertFalse(jwtUtil.validateToken("day-khong-phai-jwt-hop-le"));
    }

    @Test
    void validateToken_whenSignedWithDifferentSecret_shouldReturnFalse() {
        SecretKey forgedKey = Keys.hmacShaKeyFor(
                "completely-different-secret-key-32-bytes-minimum".getBytes(StandardCharsets.UTF_8));
        String forgedToken = Jwts.builder()
                .subject("attacker")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(forgedKey)
                .compact();

        assertFalse(jwtUtil.validateToken(forgedToken));
    }
}