package com.example.englishlearningplatform.service;

import com.example.englishlearningplatform.dto.progress.UserProgressResponse;
import com.example.englishlearningplatform.entity.*;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.UserProgressRepository;
import com.example.englishlearningplatform.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserProgressServiceTest {

    @Mock
    private UserProgressRepository userProgressRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProgressService userProgressService;

    private static final String TEST_USERNAME = "testuser";
    private User testUser;

    @BeforeEach
    void setUp() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USERNAME, null);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProgress_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userProgressService.getMyProgress());

        verify(userRepository).findByUsername(TEST_USERNAME);
        verifyNoInteractions(userProgressRepository);
    }

    @Test
    void getMyProgress_happyPath_shouldReturnMappedList() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        Topic topic1 = new Topic();
        topic1.setId(10L);
        topic1.setTitle("Topic 1");

        Topic topic2 = new Topic();
        topic2.setId(20L);
        topic2.setTitle("Topic 2");

        UserProgress progress1 = new UserProgress();
        progress1.setId(100L);
        progress1.setUser(testUser);
        progress1.setTopic(topic1);
        progress1.setProgressPercent(50);
        progress1.setStatus(ProgressStatus.IN_PROGRESS);

        UserProgress progress2 = new UserProgress();
        progress2.setId(200L);
        progress2.setUser(testUser);
        progress2.setTopic(topic2);
        progress2.setProgressPercent(100);
        progress2.setStatus(ProgressStatus.COMPLETED);

        when(userProgressRepository.findByUser_Id(testUser.getId()))
                .thenReturn(List.of(progress1, progress2));

        List<UserProgressResponse> result = userProgressService.getMyProgress();

        assertNotNull(result);
        assertEquals(2, result.size());

        UserProgressResponse res1 = result.get(0);
        assertEquals(50, res1.getProgressPercent());
        assertEquals(ProgressStatus.IN_PROGRESS, res1.getStatus());

        UserProgressResponse res2 = result.get(1);
        assertEquals(100, res2.getProgressPercent());
        assertEquals(ProgressStatus.COMPLETED, res2.getStatus());

        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userProgressRepository).findByUser_Id(testUser.getId());
    }

    @Test
    void getMyProgress_whenNoProgressRecords_shouldReturnEmptyList() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        when(userProgressRepository.findByUser_Id(testUser.getId()))
                .thenReturn(Collections.emptyList());

        List<UserProgressResponse> result = userProgressService.getMyProgress();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userProgressRepository).findByUser_Id(testUser.getId());
    }
}