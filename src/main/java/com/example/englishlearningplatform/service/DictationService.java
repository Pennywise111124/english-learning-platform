package com.example.englishlearningplatform.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.dto.dictation.AdminDictationLessonResponse;
import com.example.englishlearningplatform.dto.dictation.DictationCatalogItem;
import com.example.englishlearningplatform.dto.dictation.DictationLessonRequest;
import com.example.englishlearningplatform.dto.dictation.DictationLessonResponse;
import com.example.englishlearningplatform.dto.dictation.DictationProgress;
import com.example.englishlearningplatform.dto.dictation.DictationResultResponse;
import com.example.englishlearningplatform.dto.dictation.DictationSort;
import com.example.englishlearningplatform.dto.dictation.DictationSubmitRequest;
import com.example.englishlearningplatform.dto.dictation.DictationSubmitResponse;
import com.example.englishlearningplatform.entity.DictationLesson;
import com.example.englishlearningplatform.entity.DictationResult;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.event.FileDeletionEvent;
import com.example.englishlearningplatform.exception.ResourceConflictException;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.DictationLessonRepository;
import com.example.englishlearningplatform.repository.DictationLessonSpecifications;
import com.example.englishlearningplatform.repository.DictationLessonStats;
import com.example.englishlearningplatform.repository.DictationResultRepository;
import com.example.englishlearningplatform.repository.TopicRepository;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.service.DictationComparator.ComparisonResult;

@Service
public class DictationService {
    private final DictationLessonRepository lessonRepository;
    private final DictationResultRepository resultRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DictationService(
            DictationLessonRepository lessonRepository,
            DictationResultRepository resultRepository,
            TopicRepository topicRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.lessonRepository = lessonRepository;
        this.resultRepository = resultRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public List<DictationLessonResponse> getLessonsByTopic(Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic not found with id: " + topicId);
        }

        return lessonRepository.findByTopic_IdAndMediaUrlIsNotNullOrderByIdAsc(topicId)
                .stream()
                .map(DictationLessonResponse::from)
                .toList();
    }

    @Transactional
    public DictationSubmitResponse submit(Long lessonId, DictationSubmitRequest request) {
        User user = getCurrentUser();

        DictationLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Dictation lesson not found with id: " + lessonId));

        if (lesson.getMediaUrl() == null) {
            throw new ResourceNotFoundException("Dictation lesson not found with id: " + lessonId);
        }

        ComparisonResult cmp = DictationComparator.compare(lesson.getTranscript(), request.userInput());

        DictationResult result = new DictationResult();
        result.setUser(user);
        result.setLesson(lesson);
        result.setUserInput(request.userInput());
        result.setAccuracy(cmp.accuracy());
        result.setCreatedAt(clock.instant());

        DictationResult saved = resultRepository.save(result);

        return new DictationSubmitResponse(
                saved.getId(),
                lesson.getId(),
                cmp.accuracy(),
                lesson.getTranscript(),
                cmp.words(),
                saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<DictationResultResponse> getMyResults(Long lessonId, int page, int size) {
        User user = getCurrentUser();

        if (!lessonRepository.existsById(lessonId)) {
            throw new ResourceNotFoundException("Dictation lesson not found with id: " + lessonId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<DictationResultResponse> resultPage = resultRepository
                .findByUser_IdAndLesson_Id(user.getId(), lessonId, pageable)
                .map(DictationResultResponse::from);

        return PageResponse.from(resultPage);
    }

    // ── Admin ──
    private void validateTranscript(String transcript) {
        List<String> words = DictationComparator.tokenize(transcript);
        if (words.isEmpty()) {
            throw new IllegalArgumentException("Transcript must contain at least one word");
        }
        if (words.size() > DictationComparator.MAX_WORDS) {
            throw new IllegalArgumentException(
                    "Transcript must not exceed " + DictationComparator.MAX_WORDS + " words");
        }
    }

    @Transactional(readOnly = true)
    public List<AdminDictationLessonResponse> getLessonsForAdmin(Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic not found with id: " + topicId);
        }

        return lessonRepository.findByTopic_IdOrderByIdAsc(topicId)
                .stream()
                .map(AdminDictationLessonResponse::from)
                .toList();
    }

    @Transactional
    public AdminDictationLessonResponse createLesson(Long topicId, DictationLessonRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        validateTranscript(request.transcript());

        DictationLesson lesson = new DictationLesson();
        lesson.setTopic(topic);
        lesson.setTitle(request.title());
        lesson.setTranscript(request.transcript());
        lesson.setLevel(request.level());
        lesson.setMediaUrl(null);

        DictationLesson saved = lessonRepository.save(lesson);
        return AdminDictationLessonResponse.from(saved);
    }

    @Transactional
    public AdminDictationLessonResponse updateLesson(Long id, DictationLessonRequest request) {
        DictationLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dictation lesson not found with id: " + id));

        validateTranscript(request.transcript());

        lesson.setTitle(request.title());
        lesson.setTranscript(request.transcript());
        lesson.setLevel(request.level());

        DictationLesson saved = lessonRepository.save(lesson);
        return AdminDictationLessonResponse.from(saved);
    }

    @Transactional
    public void deleteLesson(Long id) {
        DictationLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dictation lesson not found with id: " + id));

        if (resultRepository.existsByLesson_Id(id)) {
            throw new ResourceConflictException("Cannot delete dictation lesson: users have submitted results for it");
        }

        String oldUrl = lesson.getMediaUrl();
        lessonRepository.delete(lesson);

        if (oldUrl != null && !oldUrl.isBlank()) {
            eventPublisher.publishEvent(new FileDeletionEvent(oldUrl));
        }
    }

    @Transactional
    public AdminDictationLessonResponse uploadAudio(Long id, MultipartFile file) {
        DictationLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dictation lesson not found with id: " + id));

        String oldUrl = lesson.getMediaUrl();
        String newUrl = fileStorageService.storeAudio(file, "dictation");

        lesson.setMediaUrl(newUrl);
        DictationLesson saved = lessonRepository.save(lesson);

        if (oldUrl != null && !oldUrl.isBlank()) {
            eventPublisher.publishEvent(new FileDeletionEvent(oldUrl));
        }

        return AdminDictationLessonResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<DictationCatalogItem> getCatalog(String keyword, Long topicId, Level level,
            DictationProgress progress, DictationSort sort, int page, int size) {
        User user = getCurrentUser();

        Specification<DictationLesson> spec = DictationLessonSpecifications.catalog(
                user.getId(), keyword, topicId, level, progress, sort);

        Page<DictationLesson> lessonPage = lessonRepository.findAll(spec, PageRequest.of(page, size));

        List<Long> ids = lessonPage.getContent().stream().map(DictationLesson::getId).toList();

        Map<Long, DictationLessonStats> statsByLesson = ids.isEmpty()
                ? Map.of()
                : resultRepository.findStatsByUserIdAndLessonIds(user.getId(), ids).stream()
                        .collect(Collectors.toMap(DictationLessonStats::getLessonId, s -> s));

        return PageResponse.from(lessonPage.map(l -> DictationCatalogItem.of(l, statsByLesson.get(l.getId()))));
    }
}
