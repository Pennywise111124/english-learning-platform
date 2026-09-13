package com.example.englishlearningplatform.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final TopicRepository topicRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    public FlashcardService(FlashcardRepository flashcardRepository, TopicRepository topicRepository,
            ApplicationEventPublisher eventPublisher, FileStorageService fileStorageService) {
        this.flashcardRepository = flashcardRepository;
        this.topicRepository = topicRepository;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "flashcardsByTopic", key = "#topicId")
    public List<FlashcardResponse> getFlashcardsByTopic(Long topicId) {

        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic không tồn tại với Id: " + topicId);
        }

        return flashcardRepository.findByTopicId(topicId)
                .stream()
                .map(FlashcardResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public FlashcardResponse createFlashcard(Long topicId, FlashcardCreateRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic không tồn tại với Id: " + topicId));

        Flashcard flashcard = new Flashcard();
        flashcard.setTopic(topic);
        flashcard.setWord(request.getWord());
        flashcard.setMeaning(request.getMeaning());
        flashcard.setExample(request.getExample());
        flashcard.setAudioUrl(request.getAudioUrl());

        Flashcard saveFlashcard = flashcardRepository.save(flashcard);
        eventPublisher.publishEvent(new FlashcardChangedEvent(topicId));
        return FlashcardResponse.from(saveFlashcard);
    }

    @Transactional
    public FlashcardResponse updateFlashcard(Long id, FlashcardUpdateRequest request) {
        Flashcard flashcard = flashcardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard không tồn tại với Id: " + id));

        flashcard.setWord(request.getWord());
        flashcard.setMeaning(request.getMeaning());
        flashcard.setExample(request.getExample());
        flashcard.setAudioUrl(request.getAudioUrl());

        Flashcard updatedFlashcard = flashcardRepository.save(flashcard);
        eventPublisher.publishEvent(new FlashcardChangedEvent(updatedFlashcard.getTopic().getId()));
        return FlashcardResponse.from(updatedFlashcard);
    }

    @Transactional
    public void deleteFlashcard(Long id) {
        Flashcard flashcard = flashcardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard không tồn tại với Id: " + id));

        Long topicId = flashcard.getTopic().getId();
        String oldImageUrl = flashcard.getImageUrl();

        flashcardRepository.delete(flashcard);

        eventPublisher.publishEvent(new FlashcardChangedEvent(topicId));

        if (oldImageUrl != null && !oldImageUrl.isBlank()) {
            eventPublisher.publishEvent(new FileDeletionEvent(oldImageUrl));
        }
    }

    @Transactional
    public FlashcardResponse updateFlashcardImage(Long id, MultipartFile file) {
        Flashcard flashcard = flashcardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard không tồn tại với Id: " + id));

        String oldImageUrl = flashcard.getImageUrl();

        String imageUrl = fileStorageService.storeImage(file, "flashcards");
        flashcard.setImageUrl(imageUrl);

        Flashcard updatedFlashcard = flashcardRepository.save(flashcard);

        eventPublisher.publishEvent(new FlashcardChangedEvent(updatedFlashcard.getTopic().getId()));

        if (oldImageUrl != null && !oldImageUrl.isBlank()) {
            eventPublisher.publishEvent(new FileDeletionEvent(oldImageUrl));
        }

        return FlashcardResponse.from(updatedFlashcard);
    }
}