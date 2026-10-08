package com.example.englishlearningplatform.service;

import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.dto.topic.TopicCreateRequest;
import com.example.englishlearningplatform.dto.topic.TopicResponse;
import com.example.englishlearningplatform.dto.topic.TopicSort;
import com.example.englishlearningplatform.dto.topic.TopicUpdateRequest;
import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.event.FileDeletionEvent;
import com.example.englishlearningplatform.event.TopicChangedEvent;
import com.example.englishlearningplatform.exception.ResourceConflictException;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.DictationLessonRepository;
import com.example.englishlearningplatform.repository.DictationResultRepository;
import com.example.englishlearningplatform.repository.QuizAttemptRepository;
import com.example.englishlearningplatform.repository.TopicRepository;
import com.example.englishlearningplatform.repository.UserProgressRepository;
import com.example.englishlearningplatform.repository.UserVocabularyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
    @Mock
    private UserVocabularyRepository userVocabularyRepository;
    @Mock
    private DictationLessonRepository dictationLessonRepository;
    @Mock
    private DictationResultRepository dictationResultRepository;

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
    void getTopicById_whenNotPublished_throwsNotFound() {
        when(topicRepository.findByIdAndStatus(TOPIC_ID, ContentStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> topicService.getTopicById(TOPIC_ID));

        verify(topicRepository, never()).findById(any());
    }

    @Test
    void getTopicById_happyPath_shouldReturnMappedResponse() {
        when(topicRepository.findByIdAndStatus(TOPIC_ID, ContentStatus.PUBLISHED)).thenReturn(Optional.of(testTopic));

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
    void createTopic_newTopicIsDraft() {
        TopicCreateRequest request = new TopicCreateRequest();
        request.setTitle("Grammar Basics");
        request.setLevel(Level.BEGINNER);

        when(topicRepository.existsByTitleAndLevel(request.getTitle(), request.getLevel())).thenReturn(false);
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> inv.getArgument(0));

        topicService.createTopic(request);

        ArgumentCaptor<Topic> captor = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(captor.capture());
        assertEquals(ContentStatus.DRAFT, captor.getValue().getStatus());
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
    // changeStatus()
    // ------------------------------------------------------------------

    static Stream<Arguments> allTransitions() {
        return Arrays.stream(ContentStatus.values())
                .flatMap(from -> Arrays.stream(ContentStatus.values()).map(to -> Arguments.of(from, to)));
    }

    @Test
    void changeStatus_whenNotFound_throws404() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> topicService.changeStatus(TOPIC_ID, ContentStatus.PUBLISHED));

        verify(topicRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void changeStatus_setsStatusSavesAndPublishesEventWithTopicId() {
        testTopic.setStatus(ContentStatus.DRAFT);
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> inv.getArgument(0));

        topicService.changeStatus(TOPIC_ID, ContentStatus.PUBLISHED);

        ArgumentCaptor<Topic> savedCaptor = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(savedCaptor.capture());
        Topic saved = savedCaptor.getValue();
        assertEquals(ContentStatus.PUBLISHED, saved.getStatus());
        // "Chỉ đặt status": các trường khác giữ nguyên
        assertEquals("Grammar Basics", saved.getTitle());
        assertEquals("Basic grammar concepts", saved.getDescription());
        assertEquals(Level.BEGINNER, saved.getLevel());

        ArgumentCaptor<TopicChangedEvent> eventCaptor = ArgumentCaptor.forClass(TopicChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(TOPIC_ID, eventCaptor.getValue().getTopicId());
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allTransitions")
    void changeStatus_allowsEveryTransition(ContentStatus from, ContentStatus to) {
        testTopic.setStatus(from);
        // Phải tra bằng findById (không lọc status), nếu không DRAFT/ARCHIVED sẽ không
        // bao giờ công bố được
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> topicService.changeStatus(TOPIC_ID, to));

        assertEquals(to, testTopic.getStatus());
        verify(topicRepository, never()).findByIdAndStatus(any(), any());
    }

    // ------------------------------------------------------------------
    // getTopicsForAdmin() / getTopicForAdmin()
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    void getTopicsForAdmin_mapsPageAndKeepsStatusInResponse() {
        testTopic.setStatus(ContentStatus.DRAFT);
        when(topicRepository.findAll(ArgumentMatchers.<Specification<Topic>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testTopic)));

        PageResponse<TopicResponse> response = topicService.getTopicsForAdmin(
                null, null, TopicSort.NEWEST, null, PageRequest.of(0, 20));

        assertEquals(1, response.getContent().size());
        assertEquals(ContentStatus.DRAFT, response.getContent().get(0).getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getTopicsForAdmin_stripsSortFromPageable_soSpecificationOrderingIsKept() {
        when(topicRepository.findAll(ArgumentMatchers.<Specification<Topic>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        topicService.getTopicsForAdmin(null, null, TopicSort.POPULAR, null,
                PageRequest.of(2, 5, Sort.by("title")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(topicRepository).findAll(ArgumentMatchers.<Specification<Topic>>any(), captor.capture());
        assertTrue(captor.getValue().getSort().isUnsorted());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    void getTopicForAdmin_whenNotFound_throws404() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> topicService.getTopicForAdmin(TOPIC_ID));

        verify(topicRepository, never()).findByIdAndStatus(any(), any());
    }

    @ParameterizedTest
    @EnumSource(ContentStatus.class)
    void getTopicForAdmin_returnsTopicInAnyStatus(ContentStatus status) {
        testTopic.setStatus(status);
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));

        TopicResponse response = topicService.getTopicForAdmin(TOPIC_ID);

        assertEquals(status, response.getStatus());
        verify(topicRepository, never()).findByIdAndStatus(any(), any());
    }

    @Test
    void adminReads_areNotCached() throws NoSuchMethodException {
        Method list = TopicService.class.getMethod("getTopicsForAdmin",
                String.class, Level.class, TopicSort.class, ContentStatus.class, Pageable.class);
        Method detail = TopicService.class.getMethod("getTopicForAdmin", Long.class);

        assertNull(list.getAnnotation(Cacheable.class));
        assertNull(detail.getAnnotation(Cacheable.class));
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
        verify(topicRepository, never()).delete(any(Topic.class));
    }

    @Test
    void deleteTopic_whenQuizAttemptExists_shouldThrowResourceConflictException() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(userProgressRepository.existsByTopic_Id(TOPIC_ID)).thenReturn(false);
        when(quizAttemptRepository.existsByQuiz_Topic_Id(TOPIC_ID)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> topicService.deleteTopic(TOPIC_ID));

        verify(topicRepository, never()).delete(any(Topic.class));
    }

    @Test
    void deleteTopic_whenNoImageUrl_shouldDeleteAndPublishOnlyTopicChangedEvent() {
        testTopic.setImageUrl(null);
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(userProgressRepository.existsByTopic_Id(TOPIC_ID)).thenReturn(false);
        when(quizAttemptRepository.existsByQuiz_Topic_Id(TOPIC_ID)).thenReturn(false);
        when(userVocabularyRepository.existsByFlashcard_Topic_Id(TOPIC_ID)).thenReturn(false);

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
        when(userVocabularyRepository.existsByFlashcard_Topic_Id(TOPIC_ID)).thenReturn(false);

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

    @Test
    void deleteTopic_whenSavedFlashcardExists_shouldThrowResourceConflictException() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(userProgressRepository.existsByTopic_Id(TOPIC_ID)).thenReturn(false);
        when(quizAttemptRepository.existsByQuiz_Topic_Id(TOPIC_ID)).thenReturn(false);
        when(userVocabularyRepository.existsByFlashcard_Topic_Id(TOPIC_ID)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> topicService.deleteTopic(TOPIC_ID));

        verify(topicRepository, never()).delete(any(Topic.class));
    }

    @Test
    void deleteTopic_whenDictationResultExists_shouldThrowResourceConflictException() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(userProgressRepository.existsByTopic_Id(TOPIC_ID)).thenReturn(false);
        when(quizAttemptRepository.existsByQuiz_Topic_Id(TOPIC_ID)).thenReturn(false);
        when(userVocabularyRepository.existsByFlashcard_Topic_Id(TOPIC_ID)).thenReturn(false);
        when(dictationResultRepository.existsByLesson_Topic_Id(TOPIC_ID)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> topicService.deleteTopic(TOPIC_ID));

        verify(topicRepository, never()).delete(any(Topic.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deleteTopic_whenLessonsHaveAudio_shouldReadUrlsBeforeDeleteAndPublishEventForEachAudio() {
        testTopic.setImageUrl(null);
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(userProgressRepository.existsByTopic_Id(TOPIC_ID)).thenReturn(false);
        when(quizAttemptRepository.existsByQuiz_Topic_Id(TOPIC_ID)).thenReturn(false);
        when(userVocabularyRepository.existsByFlashcard_Topic_Id(TOPIC_ID)).thenReturn(false);
        when(dictationResultRepository.existsByLesson_Topic_Id(TOPIC_ID)).thenReturn(false);
        when(dictationLessonRepository.findMediaUrlsByTopicId(TOPIC_ID))
                .thenReturn(List.of("/uploads/dictation/a.mp3", "/uploads/dictation/b.mp3"));

        topicService.deleteTopic(TOPIC_ID);

        // Khoá quy tắc "đọc URL trước khi xoá"
        InOrder inOrder = inOrder(dictationLessonRepository, topicRepository);
        inOrder.verify(dictationLessonRepository).findMediaUrlsByTopicId(TOPIC_ID);
        inOrder.verify(topicRepository).delete(testTopic);

        // 1 TopicChangedEvent + 2 FileDeletionEvent
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(3)).publishEvent(eventCaptor.capture());

        List<String> deletedUrls = eventCaptor.getAllValues().stream()
                .filter(FileDeletionEvent.class::isInstance)
                .map(FileDeletionEvent.class::cast)
                .map(FileDeletionEvent::getFileUrl)
                .toList();
        assertEquals(List.of("/uploads/dictation/a.mp3", "/uploads/dictation/b.mp3"), deletedUrls);
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