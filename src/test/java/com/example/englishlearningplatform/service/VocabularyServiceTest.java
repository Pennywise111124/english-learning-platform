package com.example.englishlearningplatform.service;

import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.dto.vocabulary.VocabularyResponse;
import com.example.englishlearningplatform.entity.Flashcard;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.entity.UserVocabulary;
import com.example.englishlearningplatform.entity.VocabularyStatus;
import com.example.englishlearningplatform.exception.ResourceConflictException;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.FlashcardRepository;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.repository.UserVocabularyRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VocabularyServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-15T00:00:00Z");

    @Mock
    private UserVocabularyRepository vocabularyRepository;

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private VocabularyService vocabularyService;

    private UserVocabulary vocab;
    private User testUser;
    private Flashcard testFlashcard;

    @BeforeEach
    void setUp() {
        vocab = new UserVocabulary();
        vocab.setId(100L);
        vocab.setReviewCount(0);
        vocab.setDifficulty(0);
        vocab.setIntervalLevel(0);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testFlashcard = new Flashcard();
        testFlashcard.setId(10L);
        testFlashcard.setWord("hello");
        testFlashcard.setMeaning("xin chào");
    }

    private void mockSecurityUser(User user) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getUsername());
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getAllSaved_shouldReturnPagedResultSortedByIdDesc() {
        mockSecurityUser(testUser);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));

        UserVocabulary v2 = new UserVocabulary();
        v2.setId(2L);
        v2.setFlashcard(testFlashcard);
        v2.setStatus(VocabularyStatus.NEW);

        Page<UserVocabulary> page = new PageImpl<>(List.of(v2));
        when(vocabularyRepository.findByUser_Id(eq(testUser.getId()), any(Pageable.class))).thenReturn(page);

        PageResponse<VocabularyResponse> result = vocabularyService.getAllSaved(0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(2L, result.getContent().get(0).getId());
    }

    // ==========================================
    // Tests cho applyReview (Pure Domain Logic)
    // ==========================================

    @Test
    void applyReview_whenRememberedAtLevel0_shouldAdvanceTo1DayAndLearning() {
        VocabularyService.applyReview(vocab, true, FIXED_NOW);

        assertEquals(1, vocab.getReviewCount());
        assertEquals(FIXED_NOW, vocab.getLastReviewedAt());
        assertEquals(FIXED_NOW.plus(1, ChronoUnit.DAYS), vocab.getNextReviewAt());
        assertEquals(1, vocab.getIntervalLevel());
        assertEquals(VocabularyStatus.LEARNING, vocab.getStatus());
    }

    @Test
    void applyReview_whenRememberedAtLevel4_shouldStayAtLevel4AndBecomeKnown() {
        vocab.setIntervalLevel(4);

        VocabularyService.applyReview(vocab, true, FIXED_NOW);

        assertEquals(FIXED_NOW.plus(30, ChronoUnit.DAYS), vocab.getNextReviewAt());
        assertEquals(4, vocab.getIntervalLevel());
        assertEquals(VocabularyStatus.KNOWN, vocab.getStatus());
    }

    @Test
    void applyReview_whenForgottenAtAnyLevel_shouldResetToLevel0AndIncreaseDifficulty() {
        vocab.setIntervalLevel(3);
        vocab.setDifficulty(2);

        VocabularyService.applyReview(vocab, false, FIXED_NOW);

        assertEquals(0, vocab.getIntervalLevel());
        assertEquals(3, vocab.getDifficulty());
        assertEquals(FIXED_NOW.plus(1, ChronoUnit.DAYS), vocab.getNextReviewAt());
        assertEquals(VocabularyStatus.LEARNING, vocab.getStatus());
    }

    @Test
    void applyReview_whenForgottenAtLevel0_shouldStayAtLevel0() {
        vocab.setIntervalLevel(0);
        vocab.setDifficulty(0);

        VocabularyService.applyReview(vocab, false, FIXED_NOW);

        assertEquals(0, vocab.getIntervalLevel()); // Không bị âm
        assertEquals(1, vocab.getDifficulty());
        assertEquals(FIXED_NOW.plus(1, ChronoUnit.DAYS), vocab.getNextReviewAt());
        assertEquals(VocabularyStatus.LEARNING, vocab.getStatus());
    }

    // ==========================================
    // Tests cho saveWord
    // ==========================================

    @Test
    void saveWord_whenAlreadySaved_shouldThrowResourceConflictException() {
        mockSecurityUser(testUser);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(flashcardRepository.findById(testFlashcard.getId())).thenReturn(Optional.of(testFlashcard));
        when(vocabularyRepository.existsByUser_IdAndFlashcard_Id(testUser.getId(), testFlashcard.getId()))
                .thenReturn(true);

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> vocabularyService.saveWord(testFlashcard.getId()));

        assertEquals("Word is already saved", exception.getMessage());
        verify(vocabularyRepository, never()).save(any());
    }

    @Test
    void saveWord_whenFlashcardNotFound_shouldThrowResourceNotFoundException() {
        mockSecurityUser(testUser);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(flashcardRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> vocabularyService.saveWord(999L));

        assertTrue(exception.getMessage().contains("Flashcard not found"));
        verify(vocabularyRepository, never()).save(any());
    }

    @Test
    void saveWord_success() {
        mockSecurityUser(testUser);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(flashcardRepository.findById(testFlashcard.getId())).thenReturn(Optional.of(testFlashcard));
        when(vocabularyRepository.existsByUser_IdAndFlashcard_Id(testUser.getId(), testFlashcard.getId()))
                .thenReturn(false);
        when(clock.instant()).thenReturn(FIXED_NOW);

        UserVocabulary savedVocab = new UserVocabulary();
        savedVocab.setId(100L);
        savedVocab.setUser(testUser);
        savedVocab.setFlashcard(testFlashcard);
        savedVocab.setStatus(VocabularyStatus.NEW);

        when(vocabularyRepository.save(any(UserVocabulary.class))).thenReturn(savedVocab);

        VocabularyResponse response = vocabularyService.saveWord(testFlashcard.getId());

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("hello", response.getWord());
    }

    // ==========================================
    // Tests cho review
    // ==========================================

    @Test
    void review_whenNotOwner_shouldThrowResourceNotFoundException() {
        mockSecurityUser(testUser);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));

        when(vocabularyRepository.findByIdAndUser_Id(100L, testUser.getId()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> vocabularyService.review(100L, true));

        assertTrue(exception.getMessage().contains("Vocabulary not found"));
        verify(vocabularyRepository, never()).save(any());
    }

    @Test
    void getSavedFlashcardIds_shouldReturnOnlyIdsOfCurrentUser() {
        mockSecurityUser(testUser);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(vocabularyRepository.findFlashcardIdsByUserIdAndTopicId(testUser.getId(), 5L))
                .thenReturn(List.of(10L, 11L));

        Set<Long> result = vocabularyService.getSavedFlashcardIds(5L);

        assertEquals(Set.of(10L, 11L), result);
    }

    @Test
    void deleteSavedWord_whenOwner_shouldDelete() {
        mockSecurityUser(testUser);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(vocabularyRepository.findByIdAndUser_Id(100L, testUser.getId())).thenReturn(Optional.of(vocab));

        vocabularyService.deleteSavedWord(100L);

        verify(vocabularyRepository).delete(vocab);
    }

    @Test
    void deleteSavedWord_whenNotOwner_shouldThrowResourceNotFoundException() {
        mockSecurityUser(testUser);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(vocabularyRepository.findByIdAndUser_Id(100L, testUser.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vocabularyService.deleteSavedWord(100L));

        verify(vocabularyRepository, never()).delete(any());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}