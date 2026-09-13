package com.example.englishlearningplatform.service;

import com.example.englishlearningplatform.dto.topic.TopicCreateRequest;
import com.example.englishlearningplatform.dto.topic.TopicResponse;
import com.example.englishlearningplatform.dto.topic.TopicUpdateRequest;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.event.FileDeletionEvent;
import com.example.englishlearningplatform.event.TopicChangedEvent;
import com.example.englishlearningplatform.exception.ResourceConflictException;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.QuizAttemptRepository;
import com.example.englishlearningplatform.repository.TopicRepository;
import com.example.englishlearningplatform.repository.UserProgressRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;
    @Mock
    private UserProgressRepository userProgressRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private TopicService topicService;

    private static final Long TOPIC_ID = 10L;
    private Topic testTopic;

    @BeforeEach
    void setUp() {
        testTopic = new Topic();
        testTopic.setId(TOPIC_ID);
        testTopic.setTitle("Grammar Basics");
        testTopic.setLevel(Level.BEGINNER);
        testTopic.setDescription("Basic grammar concepts");
    }

    private DataIntegrityViolationException uniqueConstraintViolation(String constraintName) {
        return new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("Detail: Key already exists. " + constraintName));
    }

    // ------------------------------------------------------------------
    // getTopicById()
    // ------------------------------------------------------------------

    @Test
    void getTopicById_whenNotFound_shouldThrowResourceNotFoundException() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> topicService.getTopicById(TOPIC_ID));
    }

    @Test
    void getTopicById_happyPath_shouldReturnMappedResponse() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));

        TopicResponse response = topicService.getTopicById(TOPIC_ID);

        assertNotNull(response);
        assertEquals(TOPIC_ID, response.getId());
        assertEquals("Grammar Basics", response.getTitle());
    }

    // ------------------------------------------------------------------
    // createTopic()
    // ------------------------------------------------------------------

    @Test
    void createTopic_whenTitleAndLevelAlreadyExist_shouldThrowIllegalArgumentException() {
        TopicCreateRequest request = new TopicCreateRequest();
        request.setTitle("Grammar Basics");
        request.setLevel(Level.BEGINNER);

        when(topicRepository.existsByTitleAndLevel(request.getTitle(), request.getLevel())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> topicService.createTopic(request));

        verify(topicRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createTopic_happyPath_shouldSaveAndPublishEventWithNullTopicId() {
        TopicCreateRequest request = new TopicCreateRequest();
        request.setTitle("Grammar Basics");
        request.setLevel(Level.BEGINNER);

        when(topicRepository.existsByTitleAndLevel(request.getTitle(), request.getLevel())).thenReturn(false);
        when(topicRepository.save(any(Topic.class))).thenReturn(testTopic);

        TopicResponse response = topicService.createTopic(request);

        assertNotNull(response);
        assertEquals("Grammar Basics", response.getTitle());

        ArgumentCaptor<TopicChangedEvent> eventCaptor = ArgumentCaptor.forClass(TopicChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertNull(eventCaptor.getValue().getTopicId());
    }

    @Test
    void createTopic_whenSaveThrowsDataIntegrityViolationWithMatchingConstraint_shouldThrowIllegalArgumentException() {
        TopicCreateRequest request = new TopicCreateRequest();
        request.setTitle("Grammar Basics");
        request.setLevel(Level.BEGINNER);

        when(topicRepository.existsByTitleAndLevel(request.getTitle(), request.getLevel())).thenReturn(false);
        when(topicRepository.save(any(Topic.class)))
                .thenThrow(uniqueConstraintViolation("uq_topics_title_level"));

        assertThrows(IllegalArgumentException.class, () -> topicService.createTopic(request));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createTopic_whenSaveThrowsDataIntegrityViolationWithOtherConstraint_shouldRethrowOriginalException() {
        TopicCreateRequest request = new TopicCreateRequest();
        request.setTitle("Grammar Basics");
        request.setLevel(Level.BEGINNER);

        when(topicRepository.existsByTitleAndLevel(request.getTitle(), request.getLevel())).thenReturn(false);
        when(topicRepository.save(any(Topic.class)))
                .thenThrow(uniqueConstraintViolation("uq_some_other_table"));

        assertThrows(DataIntegrityViolationException.class, () -> topicService.createTopic(request));
    }

    // ------------------------------------------------------------------
    // updateTopic()
    // ------------------------------------------------------------------

    @Test
    void updateTopic_whenTopicNotFound_shouldThrowResourceNotFoundException() {
        TopicUpdateRequest request = new TopicUpdateRequest();
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> topicService.updateTopic(TOPIC_ID, request));
    }

    @Test
    void updateTopic_whenTitleAndLevelConflictWithAnotherTopic_shouldThrowIllegalArgumentException() {
        TopicUpdateRequest request = new TopicUpdateRequest();
        request.setTitle("Advanced Grammar");
        request.setLevel(Level.ADVANCED);

        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(topicRepository.existsByTitleAndLevelAndIdNot(request.getTitle(), request.getLevel(), TOPIC_ID))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> topicService.updateTopic(TOPIC_ID, request));
    }

    @Test
    void updateTopic_happyPath_shouldSaveAndPublishEventWithTopicId() {
        TopicUpdateRequest request = new TopicUpdateRequest();
        request.setTitle("Updated Grammar");
        request.setLevel(Level.INTERMEDIATE);

        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(topicRepository.existsByTitleAndLevelAndIdNot(request.getTitle(), request.getLevel(), TOPIC_ID))
                .thenReturn(false);
        when(topicRepository.save(any(Topic.class))).thenReturn(testTopic);

        TopicResponse response = topicService.updateTopic(TOPIC_ID, request);

        assertNotNull(response);
        ArgumentCaptor<TopicChangedEvent> eventCaptor = ArgumentCaptor.forClass(TopicChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(TOPIC_ID, eventCaptor.getValue().getTopicId());
    }

    @Test
    void updateTopic_whenSaveThrowsDataIntegrityViolationWithMatchingConstraint_shouldThrowIllegalArgumentException() {
        TopicUpdateRequest request = new TopicUpdateRequest();
        request.setTitle("Updated Grammar");
        request.setLevel(Level.INTERMEDIATE);

        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(topicRepository.existsByTitleAndLevelAndIdNot(request.getTitle(), request.getLevel(), TOPIC_ID))
                .thenReturn(false);
        when(topicRepository.save(any(Topic.class)))
                .thenThrow(uniqueConstraintViolation("uq_topics_title_level"));

        assertThrows(IllegalArgumentException.class, () -> topicService.updateTopic(TOPIC_ID, request));
    }

    // ------------------------------------------------------------------
    // deleteTopic()
    // ------------------------------------------------------------------

    @Test
    void deleteTopic_whenTopicNotFound_shouldThrowResourceNotFoundException() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> topicService.deleteTopic(TOPIC_ID));
    }

    @Test
    void deleteTopic_whenUserProgressExists_shouldThrowResourceConflictException() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(userProgressRepository.existsByTopic_Id(TOPIC_ID)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> topicService.deleteTopic(TOPIC_ID));

        verify(quizAttemptRepository, never()).existsByQuiz_Topic_Id(any());
        verify(topicRepository, never()).delete(any());
    }

    @Test
    void deleteTopic_whenQuizAttemptExists_shouldThrowResourceConflictException() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(userProgressRepository.existsByTopic_Id(TOPIC_ID)).thenReturn(false);
        when(quizAttemptRepository.existsByQuiz_Topic_Id(TOPIC_ID)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> topicService.deleteTopic(TOPIC_ID));

        verify(topicRepository, never()).delete(any());
    }

    @Test
    void deleteTopic_whenNoImageUrl_shouldDeleteAndPublishOnlyTopicChangedEvent() {
        testTopic.setImageUrl(null);
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(userProgressRepository.existsByTopic_Id(TOPIC_ID)).thenReturn(false);
        when(quizAttemptRepository.existsByQuiz_Topic_Id(TOPIC_ID)).thenReturn(false);

        topicService.deleteTopic(TOPIC_ID);

        verify(topicRepository).delete(testTopic);
        verify(eventPublisher, times(1)).publishEvent(any(TopicChangedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(FileDeletionEvent.class));
    }

    @Test
    void deleteTopic_whenImageUrlExists_shouldPublishBothEvents() {
        testTopic.setImageUrl("uploads/topics/old.jpg");
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(userProgressRepository.existsByTopic_Id(TOPIC_ID)).thenReturn(false);
        when(quizAttemptRepository.existsByQuiz_Topic_Id(TOPIC_ID)).thenReturn(false);

        topicService.deleteTopic(TOPIC_ID);

        verify(topicRepository).delete(testTopic);
        verify(eventPublisher).publishEvent(any(TopicChangedEvent.class));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        FileDeletionEvent fileEvent = eventCaptor.getAllValues().stream()
                .filter(FileDeletionEvent.class::isInstance)
                .map(FileDeletionEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("FileDeletionEvent was not published"));
        assertEquals("uploads/topics/old.jpg", fileEvent.getFileUrl());
    }

    // ------------------------------------------------------------------
    // updateTopicImage()
    // ------------------------------------------------------------------

    @Test
    void updateTopicImage_whenTopicNotFound_shouldThrowResourceNotFoundException() {
        MultipartFile file = mock(MultipartFile.class);
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> topicService.updateTopicImage(TOPIC_ID, file));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void updateTopicImage_whenNoOldImage_shouldUpdateAndPublishOnlyTopicChangedEvent() {
        testTopic.setImageUrl(null);
        MultipartFile file = mock(MultipartFile.class);

        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(fileStorageService.storeImage(file, "topics")).thenReturn("uploads/topics/new.jpg");
        when(topicRepository.save(any(Topic.class))).thenReturn(testTopic);

        TopicResponse response = topicService.updateTopicImage(TOPIC_ID, file);

        assertNotNull(response);
        verify(eventPublisher).publishEvent(any(TopicChangedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(FileDeletionEvent.class));
    }

    @Test
    void updateTopicImage_whenOldImageExists_shouldPublishFileDeletionEventForOldImage() {
        testTopic.setImageUrl("uploads/topics/old.jpg");
        MultipartFile file = mock(MultipartFile.class);

        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(fileStorageService.storeImage(file, "topics")).thenReturn("uploads/topics/new.jpg");
        when(topicRepository.save(any(Topic.class))).thenReturn(testTopic);

        topicService.updateTopicImage(TOPIC_ID, file);

        verify(eventPublisher).publishEvent(any(TopicChangedEvent.class));

        // Cung bug pattern nhu deleteTopic_whenImageUrlExists — sua giong het.
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        FileDeletionEvent fileEvent = eventCaptor.getAllValues().stream()
                .filter(FileDeletionEvent.class::isInstance)
                .map(FileDeletionEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("FileDeletionEvent was not published"));
        assertEquals("uploads/topics/old.jpg", fileEvent.getFileUrl());
    }
}