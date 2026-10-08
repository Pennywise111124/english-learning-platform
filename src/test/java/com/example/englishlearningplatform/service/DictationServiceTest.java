package com.example.englishlearningplatform.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.dto.dictation.*;
import com.example.englishlearningplatform.entity.*;
import com.example.englishlearningplatform.event.FileDeletionEvent;
import com.example.englishlearningplatform.exception.InvalidFileException;
import com.example.englishlearningplatform.exception.ResourceConflictException;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.*;

import java.time.Clock;

@ExtendWith(MockitoExtension.class)
class DictationServiceTest {

    private static final String TEST_USERNAME = "testuser";
    private static final Long TOPIC_ID = 10L;
    private static final Long LESSON_ID = 100L;
    private static final Instant FIXED_NOW = Instant.parse("2026-01-15T00:00:00Z");

    @Mock
    private DictationLessonRepository lessonRepository;
    @Mock
    private DictationResultRepository resultRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private Clock clock;

    @InjectMocks
    private DictationService dictationService;

    private User testUser;
    private Topic testTopic;

    @BeforeEach
    void setUp() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USERNAME, null);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);

        testTopic = new Topic();
        testTopic.setId(TOPIC_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private DictationLesson createLesson(Long id, String mediaUrl, String transcript) {
        DictationLesson lesson = new DictationLesson();
        lesson.setId(id);
        lesson.setTopic(testTopic);
        lesson.setTitle("Lesson " + id);
        lesson.setMediaUrl(mediaUrl);
        lesson.setTranscript(transcript);
        lesson.setLevel(Level.BEGINNER);
        return lesson;
    }

    // ------------------------------------------------------------------
    // getLessonsByTopic()
    // ------------------------------------------------------------------

    @Test
    void getLessonsByTopic_whenTopicNotPublished_throwsNotFound() {
        when(topicRepository.existsByIdAndStatus(TOPIC_ID, ContentStatus.PUBLISHED)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> dictationService.getLessonsByTopic(TOPIC_ID));

        verify(topicRepository, never()).existsById(any());
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void getLessonsByTopic_happyPath_shouldQueryOnlyLessonsWithAudio() {
        when(topicRepository.existsByIdAndStatus(TOPIC_ID, ContentStatus.PUBLISHED)).thenReturn(true);

        DictationLesson lesson1 = createLesson(101L, "/uploads/dictation/audio1.mp3", "Transcript 1");
        DictationLesson lesson2 = createLesson(102L, "/uploads/dictation/audio2.mp3", "Transcript 2");
        when(lessonRepository.findByTopic_IdAndMediaUrlIsNotNullAndStatusOrderByIdAsc(TOPIC_ID,
                ContentStatus.PUBLISHED))
                .thenReturn(List.of(lesson1, lesson2));

        List<DictationLessonResponse> response = dictationService.getLessonsByTopic(TOPIC_ID);

        assertEquals(2, response.size());
        assertEquals(101L, response.get(0).id());
        assertEquals("Lesson 101", response.get(0).title());
        assertEquals("/uploads/dictation/audio1.mp3", response.get(0).mediaUrl());

        assertEquals(102L, response.get(1).id());
        assertEquals("Lesson 102", response.get(1).title());
        assertEquals("/uploads/dictation/audio2.mp3", response.get(1).mediaUrl());

        verify(lessonRepository).findByTopic_IdAndMediaUrlIsNotNullAndStatusOrderByIdAsc(TOPIC_ID,
                ContentStatus.PUBLISHED);
    }

    // ------------------------------------------------------------------
    // submit()
    // ------------------------------------------------------------------

    @Test
    void submit_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dictationService.submit(LESSON_ID, new DictationSubmitRequest("Some input")));

        verify(resultRepository, never()).save(any());
    }

    @Test
    void submit_whenLessonNotPublished_throwsNotFound() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(lessonRepository.findByIdAndStatusAndTopic_Status(LESSON_ID, ContentStatus.PUBLISHED,
                ContentStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dictationService.submit(LESSON_ID, new DictationSubmitRequest("I like cats")));

        verify(lessonRepository, never()).findById(any());
        verify(resultRepository, never()).save(any());
    }

    @Test
    void submit_whenLessonHasNoAudio_shouldThrowResourceNotFoundExceptionAndNotSave() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        DictationLesson lessonNoAudio = createLesson(LESSON_ID, null, "I like cats");
        when(lessonRepository.findByIdAndStatusAndTopic_Status(LESSON_ID, ContentStatus.PUBLISHED,
                ContentStatus.PUBLISHED))
                .thenReturn(Optional.of(lessonNoAudio));

        assertThrows(ResourceNotFoundException.class,
                () -> dictationService.submit(LESSON_ID, new DictationSubmitRequest("I like cats")));

        verify(resultRepository, never()).save(any());
    }

    @Test
    void submit_happyPath_shouldSaveResultWithRawInputAccuracyAndClockTime() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        DictationLesson lesson = createLesson(LESSON_ID, "/uploads/dictation/audio.mp3", "I like cats");
        when(lessonRepository.findByIdAndStatusAndTopic_Status(LESSON_ID, ContentStatus.PUBLISHED,
                ContentStatus.PUBLISHED))
                .thenReturn(Optional.of(lesson));

        when(clock.instant()).thenReturn(FIXED_NOW);

        when(resultRepository.save(any(DictationResult.class))).thenAnswer(invocation -> {
            DictationResult arg = invocation.getArgument(0);
            arg.setId(500L);
            return arg;
        });

        DictationSubmitResponse response = dictationService.submit(LESSON_ID,
                new DictationSubmitRequest("I LIKE dogs"));

        ArgumentCaptor<DictationResult> captor = ArgumentCaptor.forClass(DictationResult.class);
        verify(resultRepository).save(captor.capture());

        DictationResult savedResult = captor.getValue();
        assertEquals(testUser, savedResult.getUser());
        assertEquals(lesson, savedResult.getLesson());
        assertEquals("I LIKE dogs", savedResult.getUserInput());
        assertEquals(66.7, savedResult.getAccuracy(), 0.0001);
        assertEquals(FIXED_NOW, savedResult.getCreatedAt());

        assertEquals(500L, response.resultId());
        assertEquals(LESSON_ID, response.lessonId());
        assertEquals(66.7, response.accuracy(), 0.0001);
        assertEquals("I like cats", response.transcript());
        assertEquals(3, response.words().size());
        assertEquals(FIXED_NOW, response.createdAt());
    }

    @Test
    void submit_whenInputHasNoWords_shouldThrowIllegalArgumentExceptionAndNotSave() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        DictationLesson lesson = createLesson(LESSON_ID, "/uploads/dictation/audio.mp3", "I like cats");
        when(lessonRepository.findByIdAndStatusAndTopic_Status(LESSON_ID, ContentStatus.PUBLISHED,
                ContentStatus.PUBLISHED))
                .thenReturn(Optional.of(lesson));

        assertThrows(IllegalArgumentException.class,
                () -> dictationService.submit(LESSON_ID, new DictationSubmitRequest("?!...")));

        verify(resultRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // getMyResults()
    // ------------------------------------------------------------------

    @Test
    void getMyResults_whenLessonNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(lessonRepository.existsById(LESSON_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> dictationService.getMyResults(LESSON_ID, 0, 10));

        verify(resultRepository, never()).findByUser_IdAndLesson_Id(any(), any(), any());
    }

    @Test
    void getMyResults_shouldQueryByCurrentUserIdWithNewestFirstSort() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);

        DictationLesson lesson = createLesson(LESSON_ID, "/uploads/dictation/audio.mp3", "I like cats");
        DictationResult res1 = new DictationResult();
        res1.setId(1L);
        res1.setUser(testUser);
        res1.setLesson(lesson);
        res1.setUserInput("I like cats");
        res1.setAccuracy(100.0);
        res1.setCreatedAt(FIXED_NOW);

        Page<DictationResult> page = new PageImpl<>(List.of(res1));
        when(resultRepository.findByUser_IdAndLesson_Id(eq(testUser.getId()), eq(LESSON_ID), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<DictationResultResponse> response = dictationService.getMyResults(LESSON_ID, 0, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(resultRepository).findByUser_IdAndLesson_Id(eq(testUser.getId()), eq(LESSON_ID),
                pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());

        Sort sort = pageable.getSort();
        Sort.Order createdAtOrder = sort.getOrderFor("createdAt");
        Sort.Order idOrder = sort.getOrderFor("id");

        assertNotNull(createdAtOrder);
        assertTrue(createdAtOrder.isDescending());
        assertNotNull(idOrder);
        assertTrue(idOrder.isDescending());

        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).id());
    }

    // ------------------------------------------------------------------
    // Admin: getLessonsForAdmin()
    // ------------------------------------------------------------------

    @Test
    void getLessonsForAdmin_whenTopicNotFound_shouldThrowResourceNotFoundException() {
        when(topicRepository.existsById(TOPIC_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> dictationService.getLessonsForAdmin(TOPIC_ID));
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void getLessonsForAdmin_happyPath_shouldReturnAllLessonsIncludingThoseWithoutAudio() {
        when(topicRepository.existsById(TOPIC_ID)).thenReturn(true);

        DictationLesson lessonWithAudio = createLesson(101L, "/uploads/dictation/audio.mp3", "Transcript 1");
        DictationLesson lessonNoAudio = createLesson(102L, null, "Transcript 2");

        when(lessonRepository.findByTopic_IdOrderByIdAsc(TOPIC_ID))
                .thenReturn(List.of(lessonWithAudio, lessonNoAudio));

        List<AdminDictationLessonResponse> response = dictationService.getLessonsForAdmin(TOPIC_ID);

        assertEquals(2, response.size());
        assertEquals(101L, response.get(0).id());
        assertEquals("/uploads/dictation/audio.mp3", response.get(0).mediaUrl());

        assertEquals(102L, response.get(1).id());
        assertNull(response.get(1).mediaUrl());

        verify(lessonRepository).findByTopic_IdOrderByIdAsc(TOPIC_ID);
    }

    // ------------------------------------------------------------------
    // Admin: createLesson()
    // ------------------------------------------------------------------

    @Test
    void createLesson_whenTopicNotFound_shouldThrowResourceNotFoundExceptionAndNotSave() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

        DictationLessonRequest request = new DictationLessonRequest("Title", "Valid transcript", Level.BEGINNER);
        assertThrows(ResourceNotFoundException.class, () -> dictationService.createLesson(TOPIC_ID, request));

        verify(lessonRepository, never()).save(any());
    }

    @Test
    void createLesson_whenTranscriptOnlyPunctuation_shouldThrowIllegalArgumentExceptionAndNotSave() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));

        DictationLessonRequest request = new DictationLessonRequest("Title", "?!...", Level.BEGINNER);
        assertThrows(IllegalArgumentException.class, () -> dictationService.createLesson(TOPIC_ID, request));

        verify(lessonRepository, never()).save(any());
    }

    @Test
    void createLesson_whenTranscriptExceedsMaxWords_shouldThrowIllegalArgumentExceptionAndNotSave() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));

        String longTranscript = String.join(" ", Collections.nCopies(1001, "word"));
        DictationLessonRequest request = new DictationLessonRequest("Title", longTranscript, Level.BEGINNER);

        assertThrows(IllegalArgumentException.class, () -> dictationService.createLesson(TOPIC_ID, request));

        verify(lessonRepository, never()).save(any());
    }

    @Test
    void createLesson_whenTranscriptHasExactlyMaxWords_shouldSave() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(lessonRepository.save(any(DictationLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        String maxTranscript = String.join(" ", Collections.nCopies(DictationComparator.MAX_WORDS, "word"));
        DictationLessonRequest request = new DictationLessonRequest("Title", maxTranscript, Level.BEGINNER);

        assertDoesNotThrow(() -> dictationService.createLesson(TOPIC_ID, request));
        verify(lessonRepository).save(any(DictationLesson.class));
    }

    @Test
    void createLesson_happyPath_shouldSaveLessonWithNullMediaUrl() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(testTopic));
        when(lessonRepository.save(any(DictationLesson.class))).thenAnswer(inv -> {
            DictationLesson lesson = inv.getArgument(0);
            lesson.setId(LESSON_ID);
            return lesson;
        });

        DictationLessonRequest request = new DictationLessonRequest("Title", "Valid transcript", Level.INTERMEDIATE);
        AdminDictationLessonResponse response = dictationService.createLesson(TOPIC_ID, request);

        ArgumentCaptor<DictationLesson> captor = ArgumentCaptor.forClass(DictationLesson.class);
        verify(lessonRepository).save(captor.capture());

        DictationLesson savedLesson = captor.getValue();
        assertEquals(testTopic, savedLesson.getTopic());
        assertEquals("Title", savedLesson.getTitle());
        assertEquals("Valid transcript", savedLesson.getTranscript());
        assertEquals(Level.INTERMEDIATE, savedLesson.getLevel());
        assertNull(savedLesson.getMediaUrl());
        assertEquals(ContentStatus.DRAFT, savedLesson.getStatus());

        assertEquals(LESSON_ID, response.id());
    }

    // ------------------------------------------------------------------
    // Admin: updateLesson()
    // ------------------------------------------------------------------

    @Test
    void updateLesson_whenLessonNotFound_shouldThrowResourceNotFoundException() {
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

        DictationLessonRequest request = new DictationLessonRequest("Title", "Valid transcript", Level.BEGINNER);
        assertThrows(ResourceNotFoundException.class, () -> dictationService.updateLesson(LESSON_ID, request));

        verify(lessonRepository, never()).save(any());
    }

    @Test
    void updateLesson_whenTranscriptEmpty_shouldThrowIllegalArgumentException() {
        DictationLesson lesson = createLesson(LESSON_ID, "/uploads/dictation/existing.mp3", "Old transcript");
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        DictationLessonRequest request = new DictationLessonRequest("Title", "", Level.BEGINNER);
        assertThrows(IllegalArgumentException.class, () -> dictationService.updateLesson(LESSON_ID, request));

        verify(lessonRepository, never()).save(any());
    }

    @Test
    void updateLesson_happyPath_shouldUpdateFieldsAndKeepExistingMediaUrl() {
        DictationLesson lesson = createLesson(LESSON_ID, "/uploads/dictation/existing.mp3", "Old transcript");
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(DictationLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        DictationLessonRequest request = new DictationLessonRequest("New Title", "New valid transcript",
                Level.ADVANCED);
        AdminDictationLessonResponse response = dictationService.updateLesson(LESSON_ID, request);

        assertEquals("New Title", response.title());
        assertEquals("New valid transcript", response.transcript());
        assertEquals(Level.ADVANCED, response.level());
        assertEquals("/uploads/dictation/existing.mp3", response.mediaUrl());
    }

    // ------------------------------------------------------------------
    // Admin: deleteLesson()
    // ------------------------------------------------------------------

    @Test
    void deleteLesson_whenLessonNotFound_shouldThrowResourceNotFoundException() {
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> dictationService.deleteLesson(LESSON_ID));
    }

    @Test
    void deleteLesson_whenResultExists_shouldThrowResourceConflictExceptionAndNotDelete() {
        DictationLesson lesson = createLesson(LESSON_ID, "/uploads/dictation/audio.mp3", "Transcript");
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(resultRepository.existsByLesson_Id(LESSON_ID)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> dictationService.deleteLesson(LESSON_ID));

        verify(lessonRepository, never()).deleteById(LESSON_ID);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deleteLesson_whenNoAudio_shouldDeleteWithoutPublishingFileDeletionEvent() {
        DictationLesson lesson = createLesson(LESSON_ID, null, "Transcript");
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(resultRepository.existsByLesson_Id(LESSON_ID)).thenReturn(false);

        dictationService.deleteLesson(LESSON_ID);

        verify(lessonRepository).delete(lesson);
        verify(eventPublisher, never()).publishEvent(any(FileDeletionEvent.class));
    }

    @Test
    void deleteLesson_whenHasAudio_shouldDeleteAndPublishFileDeletionEvent() {
        DictationLesson lesson = createLesson(LESSON_ID, "/uploads/dictation/old-audio.mp3", "Transcript");
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(resultRepository.existsByLesson_Id(LESSON_ID)).thenReturn(false);

        dictationService.deleteLesson(LESSON_ID);

        verify(lessonRepository).delete(lesson);

        ArgumentCaptor<FileDeletionEvent> captor = ArgumentCaptor.forClass(FileDeletionEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("/uploads/dictation/old-audio.mp3", captor.getValue().getFileUrl());
    }

    // ------------------------------------------------------------------
    // Admin: uploadAudio()
    // ------------------------------------------------------------------

    @Test
    void uploadAudio_whenLessonNotFound_shouldThrowResourceNotFoundExceptionAndNotStoreAudio() {
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());
        MultipartFile file = mock(MultipartFile.class);

        assertThrows(ResourceNotFoundException.class, () -> dictationService.uploadAudio(LESSON_ID, file));

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAudio_whenStoreAudioThrowsInvalidFileException_shouldRethrowAndNotSaveOrUpdateUrl() {
        DictationLesson lesson = createLesson(LESSON_ID, "/uploads/dictation/old-audio.mp3", "Transcript");
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        MultipartFile file = mock(MultipartFile.class);
        when(fileStorageService.storeAudio(file, "dictation"))
                .thenThrow(new InvalidFileException("Invalid audio format"));

        assertThrows(InvalidFileException.class, () -> dictationService.uploadAudio(LESSON_ID, file));

        verify(lessonRepository, never()).save(any());
        assertEquals("/uploads/dictation/old-audio.mp3", lesson.getMediaUrl());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void uploadAudio_firstTimeUpload_shouldSetNewUrlAndNotPublishFileDeletionEvent() {
        DictationLesson lesson = createLesson(LESSON_ID, null, "Transcript");
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(DictationLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        MultipartFile file = mock(MultipartFile.class);
        when(fileStorageService.storeAudio(file, "dictation")).thenReturn("/uploads/dictation/new-audio.mp3");

        AdminDictationLessonResponse response = dictationService.uploadAudio(LESSON_ID, file);

        assertEquals("/uploads/dictation/new-audio.mp3", response.mediaUrl());
        verify(eventPublisher, never()).publishEvent(any(FileDeletionEvent.class));
    }

    @Test
    void uploadAudio_replaceAudio_shouldSetNewUrlAndPublishFileDeletionEventForOldUrl() {
        DictationLesson lesson = createLesson(LESSON_ID, "/uploads/dictation/old-audio.mp3", "Transcript");
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(DictationLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        MultipartFile file = mock(MultipartFile.class);
        when(fileStorageService.storeAudio(file, "dictation")).thenReturn("/uploads/dictation/new-audio.mp3");

        AdminDictationLessonResponse response = dictationService.uploadAudio(LESSON_ID, file);

        assertEquals("/uploads/dictation/new-audio.mp3", response.mediaUrl());

        ArgumentCaptor<FileDeletionEvent> captor = ArgumentCaptor.forClass(FileDeletionEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("/uploads/dictation/old-audio.mp3", captor.getValue().getFileUrl());
    }

    // ------------------------------------------------------------------
    // Admin: changeStatus()
    // ------------------------------------------------------------------

    @Test
    void changeStatus_whenNotFound_throws404() {
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dictationService.changeStatus(LESSON_ID, ContentStatus.PUBLISHED));

        verify(lessonRepository, never()).save(any());
    }

    @Test
    void changeStatus_publishWithoutAudio_throwsConflict() {
        DictationLesson lesson = createLesson(LESSON_ID, null, "Transcript");
        lesson.setStatus(ContentStatus.DRAFT);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        assertThrows(ResourceConflictException.class,
                () -> dictationService.changeStatus(LESSON_ID, ContentStatus.PUBLISHED));

        verify(lessonRepository, never()).save(any());
        assertEquals(ContentStatus.DRAFT, lesson.getStatus());
    }

    @Test
    void changeStatus_publishWithAudio_succeeds() {
        DictationLesson lesson = createLesson(LESSON_ID, "/uploads/dictation/audio.mp3", "Transcript");
        lesson.setStatus(ContentStatus.DRAFT);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(DictationLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        dictationService.changeStatus(LESSON_ID, ContentStatus.PUBLISHED);

        ArgumentCaptor<DictationLesson> captor = ArgumentCaptor.forClass(DictationLesson.class);
        verify(lessonRepository).save(captor.capture());
        DictationLesson saved = captor.getValue();
        assertEquals(ContentStatus.PUBLISHED, saved.getStatus());
        // Chỉ đổi status: audio và transcript giữ nguyên
        assertEquals("/uploads/dictation/audio.mp3", saved.getMediaUrl());
        assertEquals("Transcript", saved.getTranscript());
    }

    @Test
    void changeStatus_archiveWithoutAudio_succeeds() {
        DictationLesson lesson = createLesson(LESSON_ID, null, "Transcript");
        lesson.setStatus(ContentStatus.DRAFT);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(DictationLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        dictationService.changeStatus(LESSON_ID, ContentStatus.ARCHIVED); // quy tắc chỉ áp cho publish

        ArgumentCaptor<DictationLesson> captor = ArgumentCaptor.forClass(DictationLesson.class);
        verify(lessonRepository).save(captor.capture());
        assertEquals(ContentStatus.ARCHIVED, captor.getValue().getStatus());
    }

    private DictationLessonStats stats(Long lessonId, long attempts, Double best, Instant last) {
        DictationLessonStats s = mock(DictationLessonStats.class);
        when(s.getLessonId()).thenReturn(lessonId);
        when(s.getAttempts()).thenReturn(attempts);
        when(s.getBestAccuracy()).thenReturn(best);
        when(s.getLastAttemptAt()).thenReturn(last);
        return s;
    }

    @SuppressWarnings("unchecked")
    private void stubLessonPage(List<DictationLesson> lessons) {
        when(lessonRepository.findAll(ArgumentMatchers.<Specification<DictationLesson>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(lessons));
    }

    @Test
    void getCatalog_shouldMergePersonalStatsAndDefaultMissingToZero() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        testTopic.setTitle("Travel");
        DictationLesson practiced = createLesson(101L, "/uploads/dictation/a.mp3", "t1");
        DictationLesson fresh = createLesson(102L, "/uploads/dictation/b.mp3", "t2");
        stubLessonPage(List.of(practiced, fresh));

        DictationLessonStats practicedStats = stats(101L, 3, 87.5, FIXED_NOW); // tạo trước
        when(resultRepository.findStatsByUserIdAndLessonIds(1L, List.of(101L, 102L)))
                .thenReturn(List.of(practicedStats));

        PageResponse<DictationCatalogItem> response = dictationService.getCatalog(null, null, null, null,
                DictationSort.NEWEST, 0, 20);

        DictationCatalogItem first = response.getContent().get(0);
        assertEquals(3, first.attempts());
        assertEquals(87.5, first.bestAccuracy(), 0.0001);
        assertEquals(FIXED_NOW, first.lastAttemptAt());
        assertEquals("Travel", first.topicTitle());

        DictationCatalogItem second = response.getContent().get(1);
        assertEquals(0, second.attempts());
        assertNull(second.bestAccuracy());
        assertNull(second.lastAttemptAt());
    }

    @Test
    void getCatalog_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dictationService.getCatalog(null, null, null, null, DictationSort.NEWEST, 0, 20));

        verifyNoInteractions(lessonRepository);
        verifyNoInteractions(resultRepository);
    }

    @Test
    void getCatalog_whenPageIsEmpty_shouldNotQueryStats() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        stubLessonPage(List.of());

        PageResponse<DictationCatalogItem> response = dictationService.getCatalog(null, null, null, null,
                DictationSort.NEWEST, 0, 20);

        assertTrue(response.getContent().isEmpty());
        verify(resultRepository, never()).findStatsByUserIdAndLessonIds(any(), any());
    }

    @Test
    void getCatalog_shouldQueryStatsForCurrentUserOnly() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        testTopic.setTitle("Travel");
        stubLessonPage(List.of(createLesson(101L, "/uploads/dictation/a.mp3", "t1")));

        DictationLessonStats s = stats(101L, 2, 90.0, FIXED_NOW); // tạo trước
        when(resultRepository.findStatsByUserIdAndLessonIds(1L, List.of(101L)))
                .thenReturn(List.of(s));

        PageResponse<DictationCatalogItem> response = dictationService.getCatalog(null, null, null, null,
                DictationSort.RECENT, 0, 20);

        assertEquals(2, response.getContent().get(0).attempts());
        verify(resultRepository).findStatsByUserIdAndLessonIds(1L, List.of(101L));
        verifyNoMoreInteractions(resultRepository);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getCatalog_shouldPassUnsortedPageableSoSpecificationOrderingIsKept() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        stubLessonPage(List.of());

        dictationService.getCatalog(null, null, null, null, DictationSort.TITLE, 2, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(lessonRepository).findAll(ArgumentMatchers.<Specification<DictationLesson>>any(), captor.capture());

        Pageable pageable = captor.getValue();
        assertTrue(pageable.getSort().isUnsorted());
        assertEquals(2, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
    }
}