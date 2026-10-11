package com.example.englishlearningplatform.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.englishlearningplatform.config.QuizProperties;
import com.example.englishlearningplatform.dto.achievement.AchievementResponse;
import com.example.englishlearningplatform.entity.AchievementCode;
import com.example.englishlearningplatform.entity.UserAchievement;
import com.example.englishlearningplatform.entity.UserStats;
import com.example.englishlearningplatform.entity.VocabularyStatus;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.DictationResultRepository;
import com.example.englishlearningplatform.repository.QuizAttemptRepository;
import com.example.englishlearningplatform.repository.UserAchievementRepository;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.repository.UserStatsRepository;
import com.example.englishlearningplatform.repository.UserVocabularyRepository;
import com.example.englishlearningplatform.service.AchievementRules.Metrics;

@Service
public class AchievementService {

    private final UserStatsRepository userStatsRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final DictationResultRepository dictationResultRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final QuizProperties quizProperties;
    private final Clock clock;

    public AchievementService(UserStatsRepository userStatsRepository,
            QuizAttemptRepository quizAttemptRepository,
            UserVocabularyRepository userVocabularyRepository,
            DictationResultRepository dictationResultRepository,
            UserAchievementRepository userAchievementRepository,
            UserRepository userRepository,
            QuizProperties quizProperties,
            Clock clock) {
        this.userStatsRepository = userStatsRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.userVocabularyRepository = userVocabularyRepository;
        this.dictationResultRepository = dictationResultRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.userRepository = userRepository;
        this.quizProperties = quizProperties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluate(Long userId) {
        UserStats stats = userStatsRepository.findById(userId).orElse(null);
        if (stats == null) {
            return;
        }

        Metrics metrics = new Metrics(
                stats.getTotalXp(),
                stats.getLongestStreak(),
                quizAttemptRepository.countDistinctPassedQuizzes(userId, quizProperties.passScore()),
                userVocabularyRepository.countByUser_IdAndStatus(userId, VocabularyStatus.KNOWN),
                dictationResultRepository.countDistinctLessonsByUserId(userId));

        for (AchievementCode code : AchievementRules.earned(metrics)) {
            userAchievementRepository.insertIfAbsent(userId, code.name(), clock.instant());
        }
    }

    @Transactional(readOnly = true)
    public List<AchievementResponse> getMyAchievements(String username) {
        Long userId = resolveUserId(username);
        Map<AchievementCode, UserAchievement> byCode = userAchievementRepository.findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(UserAchievement::getCode, Function.identity()));

        List<AchievementResponse> result = new ArrayList<>();
        for (AchievementCode code : AchievementCode.values()) {
            UserAchievement ua = byCode.get(code);
            if (ua == null) {
                result.add(new AchievementResponse(code.name(), code.getTitle(), code.getDescription(),
                        false, null, false));
            } else {
                result.add(new AchievementResponse(code.name(), code.getTitle(), code.getDescription(),
                        true, ua.getAwardedAt(), ua.getDismissedAt() != null));
            }
        }
        return result;
    }

    @Transactional
    public void dismiss(String username, String codeStr) {
        AchievementCode code = parseCode(codeStr);
        Long userId = resolveUserId(username);

        int updated = userAchievementRepository.dismiss(userId, code, clock.instant());
        if (updated == 0) {
            boolean unlocked = userAchievementRepository.findByUserId(userId).stream()
                    .anyMatch(ua -> ua.getCode() == code);
            if (!unlocked) {
                throw new ResourceNotFoundException("Achievement not unlocked: " + code.name());
            }
        }
    }

    private AchievementCode parseCode(String codeStr) {
        try {
            return AchievementCode.valueOf(codeStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResourceNotFoundException("Achievement not found: " + codeStr);
        }
    }

    private Long resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username))
                .getId();
    }
}