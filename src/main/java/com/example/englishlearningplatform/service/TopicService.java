package com.example.englishlearningplatform.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
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
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.event.TopicChangedEvent;
import com.example.englishlearningplatform.event.FileDeletionEvent;
import com.example.englishlearningplatform.exception.ResourceConflictException;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
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

    public TopicService(TopicRepository topicRepository, UserProgressRepository userProgressRepository,
            QuizAttemptRepository quizAttemptRepository, ApplicationEventPublisher eventPublisher,
            FileStorageService fileStorageService, UserVocabularyRepository userVocabularyRepository) {
        this.topicRepository = topicRepository;
        this.userProgressRepository = userProgressRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.userVocabularyRepository = userVocabularyRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "topics", condition = "#pageable.pageNumber == 0 && #keyword == null", key = "(#level != null ? #level.name() : 'ALL') + ':' + #sort.name() + ':' + #pageable.pageSize")
    public PageResponse<TopicResponse> getTopics(String keyword, Level level, TopicSort sort, Pageable pageable) {
        Specification<Topic> spec = TopicSpecifications.search(keyword, level, sort);
        Page<TopicResponse> topicPage = topicRepository.findAll(spec, pageable)
                .map(TopicResponse::from);

        return PageResponse.from(topicPage);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "topicDetails", key = "#id")
    public TopicResponse getTopicById(Long id) {
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
            throw new ResourceConflictException("Cannot delete topic: users have started learning it!");
        }

        if (quizAttemptRepository.existsByQuiz_Topic_Id(id)) {
            throw new ResourceConflictException("Cannot delete topic: related quiz attempts exist!");
        }

        if (userVocabularyRepository.existsByFlashcard_Topic_Id(id)) {
            throw new ResourceConflictException("Cannot delete topic: users have saved its flashcards");
        }

        String oldImageUrl = topic.getImageUrl();

        topicRepository.delete(topic);
        eventPublisher.publishEvent(new TopicChangedEvent(id));

        if (oldImageUrl != null && !oldImageUrl.isBlank()) {
            eventPublisher.publishEvent(new FileDeletionEvent(oldImageUrl));
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
}