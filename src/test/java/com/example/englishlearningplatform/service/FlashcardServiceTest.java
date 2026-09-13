package com.example.englishlearningplatform.service;

import com.example.englishlearningplatform.dto.topic.FlashcardCreateRequest;
import com.example.englishlearningplatform.dto.topic.FlashcardResponse;
import com.example.englishlearningplatform.dto.topic.FlashcardUpdateRequest;
import com.example.englishlearningplatform.entity.Flashcard;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.event.FileDeletionEvent;
import com.example.englishlearningplatform.event.FlashcardChangedEvent;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.FlashcardRepository;
import com.example.englishlearningplatform.repository.TopicRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {

    @Mock
    private FlashcardRepository flashcardRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FlashcardService flashcardService;

    private static final Long TOPIC_ID = 10L;
    private static final Long FLASHCARD_ID = 100L;

    private Topic testTopic;
    private Flashcard testFlashcard;

    @BeforeEach
    void setUp() {
        testTopic = new Topic();
        testTopic.setId(TOPIC_ID);
        testTopic.setTitle("Grammar Basics");

        testFlashcard = new Flashcard();
        testFlashcard.setId(FLASHCARD_ID);
        testFlashcard.setTopic(testTopic);
        testFlashcard.setWord("Hello");
        testFlashcard.setMeaning("Xin chào");
    }

    // ------------------------------------------------------------------
    // getFlashcardsByTopic()
    // ------------------------------------------------------------------

    @Test
    void getFlashcardsByTopic_whenTopicNotFound_shouldThrowResourceNotFoundException() {
        when(topicRepository.existsById(TOPIC_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> flashcardService.getFlashcardsByTopic(TOPIC_ID));

        verify(flashcardRepository, never()).findByTopicId(any());
    }

    @Test
    void getFlashcardsByTopic_happyPath_shouldReturnMappedList() {
        when(topicRepository.existsById(TOPIC_ID)).thenReturn(true);
        when(flashcardRepository.findByTopicId(TOPIC_ID)).thenReturn(List.of(testFlashcard));

        List<FlashcardResponse> responses = flashcardService.getFlashcardsByTopic(TOPIC_ID);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Hello", responses.get(0).getWord());
    }

    // ------------------------------------------------------------------
    // createFlashcard()
    // ------------------------------------------------------------------

    @Test
    void createFlashcard_whenTopicNotFound_shouldThrowResourceNotFoundException() {
        FlashcardCreateRequest request = new FlashcardCreateRequest();
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> flashcardService.createFlashcard(TOPIC_ID, request));

        verify(flashcardRepository, never()).save(any());
    }

    @Test
    void createFlashcard_happyPath_shouldSaveAndPublishEventWithTopicId() {
        FlashcardCreateRequest request = new FlashcardCreateRequest();
        request.setWord("Hello");
        request.setMeaning("Xin chào");

        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);

        FlashcardResponse response = flashcardService.createFlashcard(TOPIC_ID, request);

        assertNotNull(response);
        assertEquals("Hello", response.getWord());

        ArgumentCaptor<FlashcardChangedEvent> eventCaptor = ArgumentCaptor.forClass(FlashcardChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(TOPIC_ID, eventCaptor.getValue().getTopicId());
    }

    // ------------------------------------------------------------------
    // updateFlashcard()
    // ------------------------------------------------------------------

    @Test
    void updateFlashcard_whenNotFound_shouldThrowResourceNotFoundException() {
        FlashcardUpdateRequest request = new FlashcardUpdateRequest();
        when(flashcardRepository.findById(FLASHCARD_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> flashcardService.updateFlashcard(FLASHCARD_ID, request));
    }

    @Test
    void updateFlashcard_happyPath_shouldPublishEventWithTopicIdFromSavedFlashcard() {
        FlashcardUpdateRequest request = new FlashcardUpdateRequest();
        request.setWord("Hi");
        request.setMeaning("Xin chào");

        when(flashcardRepository.findById(FLASHCARD_ID)).thenReturn(Optional.of(testFlashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);

        FlashcardResponse response = flashcardService.updateFlashcard(FLASHCARD_ID, request);

        assertNotNull(response);

        ArgumentCaptor<FlashcardChangedEvent> eventCaptor = ArgumentCaptor.forClass(FlashcardChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(TOPIC_ID, eventCaptor.getValue().getTopicId());
    }

    // ------------------------------------------------------------------
    // deleteFlashcard()
    // ------------------------------------------------------------------

    @Test
    void deleteFlashcard_whenNotFound_shouldThrowResourceNotFoundException() {
        when(flashcardRepository.findById(FLASHCARD_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> flashcardService.deleteFlashcard(FLASHCARD_ID));
    }

    @Test
    void deleteFlashcard_whenNoImageUrl_shouldDeleteAndPublishOnlyFlashcardChangedEvent() {
        testFlashcard.setImageUrl(null);
        when(flashcardRepository.findById(FLASHCARD_ID)).thenReturn(Optional.of(testFlashcard));

        flashcardService.deleteFlashcard(FLASHCARD_ID);

        verify(flashcardRepository).delete(testFlashcard);

        ArgumentCaptor<FlashcardChangedEvent> eventCaptor = ArgumentCaptor.forClass(FlashcardChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(TOPIC_ID, eventCaptor.getValue().getTopicId());

        verify(eventPublisher, never()).publishEvent(any(FileDeletionEvent.class));
    }

    @Test
    void deleteFlashcard_whenImageUrlExists_shouldPublishBothEvents() {
        testFlashcard.setImageUrl("uploads/flashcards/old.jpg");
        when(flashcardRepository.findById(FLASHCARD_ID)).thenReturn(Optional.of(testFlashcard));

        flashcardService.deleteFlashcard(FLASHCARD_ID);

        verify(flashcardRepository).delete(testFlashcard);
        verify(eventPublisher).publishEvent(any(FlashcardChangedEvent.class));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        FileDeletionEvent fileEvent = eventCaptor.getAllValues().stream()
                .filter(FileDeletionEvent.class::isInstance)
                .map(FileDeletionEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("FileDeletionEvent was not published"));

        assertEquals("uploads/flashcards/old.jpg", fileEvent.getFileUrl());
    }

    // ------------------------------------------------------------------
    // updateFlashcardImage()
    // ------------------------------------------------------------------

    @Test
    void updateFlashcardImage_whenNotFound_shouldThrowResourceNotFoundException() {
        MultipartFile file = mock(MultipartFile.class);
        when(flashcardRepository.findById(FLASHCARD_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> flashcardService.updateFlashcardImage(FLASHCARD_ID, file));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void updateFlashcardImage_whenNoOldImage_shouldUpdateAndPublishOnlyFlashcardChangedEvent() {
        testFlashcard.setImageUrl(null);
        MultipartFile file = mock(MultipartFile.class);

        when(flashcardRepository.findById(FLASHCARD_ID)).thenReturn(Optional.of(testFlashcard));
        when(fileStorageService.storeImage(file, "flashcards")).thenReturn("uploads/flashcards/new.jpg");
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);

        FlashcardResponse response = flashcardService.updateFlashcardImage(FLASHCARD_ID, file);

        assertNotNull(response);
        verify(eventPublisher).publishEvent(any(FlashcardChangedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(FileDeletionEvent.class));
    }

    @Test
    void updateFlashcardImage_whenOldImageExists_shouldPublishFileDeletionEventForOldImage() {
        testFlashcard.setImageUrl("uploads/flashcards/old.jpg");
        MultipartFile file = mock(MultipartFile.class);

        when(flashcardRepository.findById(FLASHCARD_ID)).thenReturn(Optional.of(testFlashcard));
        when(fileStorageService.storeImage(file, "flashcards")).thenReturn("uploads/flashcards/new.jpg");
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(testFlashcard);

        flashcardService.updateFlashcardImage(FLASHCARD_ID, file);

        verify(eventPublisher).publishEvent(any(FlashcardChangedEvent.class));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        FileDeletionEvent fileEvent = eventCaptor.getAllValues().stream()
                .filter(FileDeletionEvent.class::isInstance)
                .map(FileDeletionEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("FileDeletionEvent was not published"));

        assertEquals("uploads/flashcards/old.jpg", fileEvent.getFileUrl());
    }
}