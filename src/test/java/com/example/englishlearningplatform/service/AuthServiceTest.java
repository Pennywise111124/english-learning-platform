package com.example.englishlearningplatform.service;

import com.example.englishlearningplatform.dto.auth.AuthResponse;
import com.example.englishlearningplatform.dto.auth.LoginRequest;
import com.example.englishlearningplatform.dto.auth.RegisterRequest;
import com.example.englishlearningplatform.entity.RefreshToken;
import com.example.englishlearningplatform.entity.Role;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.repository.RefreshTokenRepository;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private static final String USERNAME = "testuser";
    private static final String EMAIL = "test@example.com";
    private static final String RAW_PASSWORD = "plainPassword123";
    private static final String ENCODED_PASSWORD = "$2a$encodedHash";

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(USERNAME);
        testUser.setEmail(EMAIL);
        testUser.setPasswordHash(ENCODED_PASSWORD);
        testUser.setRole(Role.USER);
    }

    // ------------------------------------------------------------------
    // register()
    // ------------------------------------------------------------------

    @Test
    void register_whenUsernameAlreadyExists_shouldThrowIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(USERNAME);
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);

        when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenEmailAlreadyExists_shouldThrowIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(USERNAME);
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);

        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_happyPath_shouldEncodePasswordAndSaveUserWithRoleUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(USERNAME);
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);

        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(USERNAME, savedUser.getUsername());
        assertEquals(EMAIL, savedUser.getEmail());
        assertEquals(ENCODED_PASSWORD, savedUser.getPasswordHash());
        assertNotEquals(RAW_PASSWORD, savedUser.getPasswordHash());
        assertEquals(Role.USER, savedUser.getRole());
    }

    // ------------------------------------------------------------------
    // login()
    // ------------------------------------------------------------------

    @Test
    void login_whenUserNotFound_shouldThrowIllegalArgumentException() {
        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(RAW_PASSWORD);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));
        assertTrue(exception.getMessage().contains("Invalid credentials"));
    }

    @Test
    void login_whenPasswordDoesNotMatch_shouldThrowIllegalArgumentException() {
        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(RAW_PASSWORD);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(RAW_PASSWORD, testUser.getPasswordHash())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_happyPath_shouldReturnTokensAndSaveRefreshToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(RAW_PASSWORD);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(RAW_PASSWORD, testUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateAccessToken(testUser.getUsername(), testUser.getRole().name()))
                .thenReturn("mock-access-token");
        when(jwtUtil.generateRefreshToken(testUser.getUsername()))
                .thenReturn("mock-refresh-token");
        when(jwtUtil.getRefreshTokenExpirationsMs())
                .thenReturn(3_600_000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertNotNull(response.getUser());
        assertEquals(USERNAME, response.getUser().getUsername());

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertEquals(testUser, savedToken.getUser());
        assertEquals("mock-refresh-token", savedToken.getToken());
        assertFalse(savedToken.isRevoked());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));
    }

    // ------------------------------------------------------------------
    // refreshAccessToken()
    // ------------------------------------------------------------------

    @Test
    void refreshAccessToken_whenTokenNotFound_shouldThrowIllegalArgumentException() {
        String tokenStr = "non-existent-token";
        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.refreshAccessToken(tokenStr));
    }

    @Test
    void refreshAccessToken_whenTokenIsRevoked_shouldThrowIllegalArgumentException() {
        String tokenStr = "revoked-token";
        RefreshToken revokedToken = new RefreshToken();
        revokedToken.setToken(tokenStr);
        revokedToken.setUser(testUser);
        revokedToken.setRevoked(true);
        revokedToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(revokedToken));

        assertThrows(IllegalArgumentException.class, () -> authService.refreshAccessToken(tokenStr));
    }

    @Test
    void refreshAccessToken_whenTokenIsExpired_shouldThrowIllegalArgumentException() {
        String tokenStr = "expired-token";
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken(tokenStr);
        expiredToken.setUser(testUser);
        expiredToken.setRevoked(false);
        expiredToken.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(expiredToken));

        assertThrows(IllegalArgumentException.class, () -> authService.refreshAccessToken(tokenStr));
    }

    @Test
    void refreshAccessToken_happyPath_shouldReturnNewAccessToken() {
        String tokenStr = "valid-refresh-token";
        RefreshToken validToken = new RefreshToken();
        validToken.setToken(tokenStr);
        validToken.setUser(testUser);
        validToken.setRevoked(false);
        validToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(validToken));
        when(jwtUtil.generateAccessToken(testUser.getUsername(), testUser.getRole().name()))
                .thenReturn("new-access-token");

        String newToken = authService.refreshAccessToken(tokenStr);

        assertEquals("new-access-token", newToken);
        verify(jwtUtil).generateAccessToken(testUser.getUsername(), testUser.getRole().name());
    }
}