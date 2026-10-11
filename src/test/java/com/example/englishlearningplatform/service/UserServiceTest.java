package com.example.englishlearningplatform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.englishlearningplatform.dto.auth.UpdateLeaderboardVisibilityRequest;
import com.example.englishlearningplatform.dto.auth.UpdateTimezoneRequest;
import com.example.englishlearningplatform.dto.auth.UserResponse;
import com.example.englishlearningplatform.entity.Role;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("testuser", null));
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.USER);
        lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UpdateTimezoneRequest request(String tz) {
        UpdateTimezoneRequest r = new UpdateTimezoneRequest();
        r.setTimezone(tz);
        return r;
    }

    @Test
    void updateMyTimezone_valid_savesTrimmedZoneAndReturnsIt() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateMyTimezone(request("  Asia/Tokyo "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("Asia/Tokyo", captor.getValue().getTimezone());
        assertEquals("Asia/Tokyo", response.getTimezone());
    }

    @Test
    void updateMyTimezone_invalid_throwsAndDoesNotSave() {
        String before = user.getTimezone();

        assertThrows(IllegalArgumentException.class, () -> userService.updateMyTimezone(request("Mars/Base")));

        verify(userRepository, never()).save(any());
        assertEquals(before, user.getTimezone());
    }

    private UpdateLeaderboardVisibilityRequest visibilityRequest(Boolean visible) {
        UpdateLeaderboardVisibilityRequest r = new UpdateLeaderboardVisibilityRequest();
        r.setVisible(visible);
        return r;
    }

    @Test
    void updateMyLeaderboardVisibility_false_savesFalseAndReturnsIt() {
        user.setLeaderboardVisible(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateMyLeaderboardVisibility(visibilityRequest(false));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertFalse(captor.getValue().isLeaderboardVisible());
        assertFalse(response.isLeaderboardVisible());
    }

    @Test
    void updateMyLeaderboardVisibility_true_savesTrueAndReturnsIt() {
        user.setLeaderboardVisible(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateMyLeaderboardVisibility(visibilityRequest(true));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().isLeaderboardVisible());
        assertTrue(response.isLeaderboardVisible());
    }
}