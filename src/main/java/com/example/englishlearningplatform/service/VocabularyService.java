package com.example.englishlearningplatform.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class VocabularyService {

    // Thang khoảng cách theo bậc interval_level 0..4
    private static final int[] INTERVAL_DAYS = { 1, 3, 7, 14, 30 };

    private final UserVocabularyRepository vocabularyRepository;
    private final FlashcardRepository flashcardRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public VocabularyService(UserVocabularyRepository vocabularyRepository,
            FlashcardRepository flashcardRepository,
            UserRepository userRepository,
            Clock clock) {
        this.vocabularyRepository = vocabularyRepository;
        this.flashcardRepository = flashcardRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<VocabularyResponse> getAllSaved(int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("id")));
        Page<UserVocabulary> vocabPage = vocabularyRepository.findByUser_Id(currentUser.getId(), pageable);
        return PageResponse.from(vocabPage.map(VocabularyResponse::from));
    }

    @Transactional(readOnly = true)
    public Set<Long> getSavedFlashcardIds(Long topicId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new HashSet<>(vocabularyRepository.findFlashcardIdsByUserIdAndTopicId(currentUser.getId(), topicId));
    }

    @Transactional
    public VocabularyResponse saveWord(Long flashcardId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Flashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found with id: " + flashcardId));

        if (vocabularyRepository.existsByUser_IdAndFlashcard_Id(currentUser.getId(), flashcardId)) {
            throw new ResourceConflictException("Word is already saved");
        }

        UserVocabulary vocab = new UserVocabulary();
        vocab.setUser(currentUser);
        vocab.setFlashcard(flashcard);
        vocab.setStatus(VocabularyStatus.NEW);
        vocab.setIntervalLevel(0);
        vocab.setReviewCount(0);
        vocab.setDifficulty(0);
        vocab.setLastReviewedAt(null);
        vocab.setNextReviewAt(clock.instant());

        try {
            UserVocabulary savedVocab = vocabularyRepository.save(vocab);
            return VocabularyResponse.from(savedVocab);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getRootCause() != null && ex.getMostSpecificCause().getMessage() != null
                    && ex.getRootCause().getMessage().contains("uq_user_vocabulary_user_flashcard")) {
                throw new ResourceConflictException("Word is already saved");
            }
            throw ex;
        }
    }

    @Transactional
    public VocabularyResponse review(Long id, boolean remembered) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserVocabulary vocab = vocabularyRepository.findByIdAndUser_Id(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary not found with id: " + id));

        applyReview(vocab, remembered, clock.instant());

        UserVocabulary updatedVocab = vocabularyRepository.save(vocab);
        return VocabularyResponse.from(updatedVocab);
    }

    static void applyReview(UserVocabulary vocab, boolean remembered, Instant now) {
        vocab.setReviewCount(vocab.getReviewCount() + 1);
        vocab.setLastReviewedAt(now);

        int currentLevel = vocab.getIntervalLevel();

        if (remembered) {
            int daysToAdd = INTERVAL_DAYS[currentLevel];
            vocab.setNextReviewAt(now.plus(daysToAdd, ChronoUnit.DAYS));

            if (currentLevel == 4) {
                vocab.setStatus(VocabularyStatus.KNOWN);
            } else {
                vocab.setStatus(VocabularyStatus.LEARNING);
            }

            vocab.setIntervalLevel(Math.min(currentLevel + 1, 4));
        } else {
            vocab.setIntervalLevel(0);
            vocab.setDifficulty(vocab.getDifficulty() + 1);
            vocab.setNextReviewAt(now.plus(1, ChronoUnit.DAYS));
            vocab.setStatus(VocabularyStatus.LEARNING);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<VocabularyResponse> getToday(int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("difficulty"),
                Sort.Order.asc("nextReviewAt"),
                Sort.Order.asc("id")));

        Page<UserVocabulary> vocabPage = vocabularyRepository
                .findByUser_IdAndNextReviewAtLessThanEqual(currentUser.getId(), clock.instant(), pageable);

        Page<VocabularyResponse> dtoPage = vocabPage.map(VocabularyResponse::from);
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public void deleteSavedWord(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserVocabulary vocab = vocabularyRepository.findByIdAndUser_Id(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary not found with id: " + id));

        vocabularyRepository.delete(vocab);
    }
}
