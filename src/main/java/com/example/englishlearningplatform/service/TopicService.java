package com.example.englishlearningplatform.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.englishlearningplatform.dto.common.PageResponse;
import com.example.englishlearningplatform.dto.topic.TopicCreateRequest;
import com.example.englishlearningplatform.dto.topic.TopicResponse;
import com.example.englishlearningplatform.dto.topic.TopicSort;
import com.example.englishlearningplatform.dto.topic.TopicUpdateRequest;
import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.event.TopicChangedEvent;
import com.example.englishlearningplatform.event.FileDeletionEvent;
import com.example.englishlearningplatform.exception.ResourceConflictException;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.DictationLessonRepository;
import com.example.englishlearningplatform.repository.DictationResultRepository;
import com.example.englishlearningplatform.repository.QuizAttemptRepository;
import com.example.englishlearningplatform.repository.TopicRepository;
import com.example.englishlearningplatform.repository.TopicSpecifications;
import com.example.englishlearningplatform.repository.UserProgressRepository;
import com.example.englishlearningplatform.repository.UserVocabularyRepository;

@Service
public class TopicService {

    private final TopicRepository topicRepository;
    private final UserProgressRepository userProgressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final UserVocabularyRepository userVocabularyRepository;
    private final DictationLessonRepository dictationLessonRepository;
    private final DictationResultRepository dictationResultRepository;

    public TopicService(TopicRepository topicRepository, UserProgressRepository userProgressRepository,
            QuizAttemptRepository quizAttemptRepository, ApplicationEventPublisher eventPublisher,
            FileStorageService fileStorageService, UserVocabularyRepository userVocabularyRepository,
            DictationLessonRepository dictationLessonRepository, DictationResultRepository dictationResultRepository) {
        this.topicRepository = topicRepository;
        this.userProgressRepository = userProgressRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.userVocabularyRepository = userVocabularyRepository;
        this.dictationLessonRepository = dictationLessonRepository;
        this.dictationResultRepository = dictationResultRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "topics", condition = "#pageable.pageNumber == 0 && #keyword == null", key = "(#level != null ? #level.name() : 'ALL') + ':' + #sort.name() + ':' + #pageable.pageSize")
    public PageResponse<TopicResponse> getTopics(String keyword, Level level, TopicSort sort, Pageable pageable) {
        Specification<Topic> spec = TopicSpecifications.forLearners(keyword, level, sort);
        Page<TopicResponse> topicPage = topicRepository.findAll(spec, pageable)
                .map(TopicResponse::from);

        return PageResponse.from(topicPage);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "topicDetails", key = "#id")
    public TopicResponse getTopicById(Long id) {
        Topic topic = topicRepository.findByIdAndStatus(id, ContentStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));

        return TopicResponse.from(topic);
    }

    @Transactional(readOnly = true)
    public PageResponse<TopicResponse> getTopicsForAdmin(String keyword, Level level, TopicSort sort,
            ContentStatus status, Pageable pageable) {
        Specification<Topic> spec = TopicSpecifications.forAdmin(keyword, level, sort, status);

        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        Page<TopicResponse> page = topicRepository.findAll(spec, unsorted).map(TopicResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public TopicResponse getTopicForAdmin(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));
        return TopicResponse.from(topic);
    }

    @Transactional
    public TopicResponse createTopic(TopicCreateRequest request) {
        if (topicRepository.existsByTitleAndLevel(request.getTitle(), request.getLevel())) {
            throw new IllegalArgumentException("A topic with this title and level already exists");
        }

        Topic topic = new Topic();
        topic.setTitle(request.getTitle());
        topic.setLevel(request.getLevel());
        topic.setDescription(request.getDescription());
        topic.setStatus(ContentStatus.DRAFT);

        try {
            Topic savedTopic = topicRepository.save(topic);
            eventPublisher.publishEvent(new TopicChangedEvent(null));
            return TopicResponse.from(savedTopic);
        } catch (DataIntegrityViolationException ex) {
            String rootMessage = ex.getMostSpecificCause().getMessage();

            if (rootMessage != null && rootMessage.contains("uq_topics_title_level")) {
                throw new IllegalArgumentException("A topic with this title and level already exists");
            }

            throw ex;
        }
    }

    @Transactional
    public TopicResponse updateTopic(Long id, TopicUpdateRequest request) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));

        if (topicRepository.existsByTitleAndLevelAndIdNot(request.getTitle(), request.getLevel(), id)) {
            throw new IllegalArgumentException("A topic with this title and level already exists");
        }

        topic.setTitle(request.getTitle());
        topic.setLevel(request.getLevel());
        topic.setDescription(request.getDescription());

        try {
            Topic savedTopic = topicRepository.save(topic);
            eventPublisher.publishEvent(new TopicChangedEvent(id));
            return TopicResponse.from(savedTopic);
        } catch (DataIntegrityViolationException ex) {
            String rootMessage = ex.getMostSpecificCause().getMessage();

            if (rootMessage != null && rootMessage.contains("uq_topics_title_level")) {
                throw new IllegalArgumentException("A topic with this title and level already exists");
            }

            throw ex;
        }
    }

    @Transactional
    public void deleteTopic(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));

        if (userProgressRepository.existsByTopic_Id(id)) {
            throw new ResourceConflictException(
                    "Cannot delete topic: users have started learning it. Archive it instead.");
        }

        if (quizAttemptRepository.existsByQuiz_Topic_Id(id)) {
            throw new ResourceConflictException(
                    "Cannot delete topic: related quiz attempts exist. Archive it instead.");
        }

        if (userVocabularyRepository.existsByFlashcard_Topic_Id(id)) {
            throw new ResourceConflictException(
                    "Cannot delete topic: users have saved its flashcards. Archive it instead.");
        }

        if (dictationResultRepository.existsByLesson_Topic_Id(id)) {
            throw new ResourceConflictException(
                    "Cannot delete topic: users have submitted dictation results for its lessons. Archive it instead.");
        }

        String oldImageUrl = topic.getImageUrl();

        List<String> lessonAudioUrls = dictationLessonRepository.findMediaUrlsByTopicId(id);

        topicRepository.delete(topic);
        eventPublisher.publishEvent(new TopicChangedEvent(id));

        if (oldImageUrl != null && !oldImageUrl.isBlank()) {
            eventPublisher.publishEvent(new FileDeletionEvent(oldImageUrl));
        }

        for (String url : lessonAudioUrls) {
            if (url != null && !url.isBlank()) {
                eventPublisher.publishEvent(new FileDeletionEvent(url));
            }
        }
    }

    @Transactional
    public TopicResponse updateTopicImage(Long id, MultipartFile file) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));

        String oldImageUrl = topic.getImageUrl();

        String imageUrl = fileStorageService.storeImage(file, "topics");
        topic.setImageUrl(imageUrl);

        Topic updatedTopic = topicRepository.save(topic);

        eventPublisher.publishEvent(new TopicChangedEvent(id));

        if (oldImageUrl != null && !oldImageUrl.isBlank()) {
            eventPublisher.publishEvent(new FileDeletionEvent(oldImageUrl));
        }

        return TopicResponse.from(updatedTopic);
    }

    @Transactional
    public TopicResponse changeStatus(Long id, ContentStatus status) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));

        topic.setStatus(status);
        Topic saved = topicRepository.save(topic);
        eventPublisher.publishEvent(new TopicChangedEvent(id));

        return TopicResponse.from(saved);
    }
}