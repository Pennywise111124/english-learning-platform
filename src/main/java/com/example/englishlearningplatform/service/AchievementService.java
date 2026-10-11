package com.example.englishlearningplatform.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.englishlearningplatform.config.QuizProperties;
import com.example.englishlearningplatform.entity.AchievementCode;
import com.example.englishlearningplatform.entity.UserStats;
import com.example.englishlearningplatform.entity.VocabularyStatus;
import com.example.englishlearningplatform.repository.DictationResultRepository;
import com.example.englishlearningplatform.repository.QuizAttemptRepository;
import com.example.englishlearningplatform.repository.UserAchievementRepository;
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
    private final QuizProperties quizProperties;
    private final Clock clock;

    public AchievementService(UserStatsRepository userStatsRepository,
            QuizAttemptRepository quizAttemptRepository,
            UserVocabularyRepository userVocabularyRepository,
            DictationResultRepository dictationResultRepository,
            UserAchievementRepository userAchievementRepository,
            QuizProperties quizProperties,
            Clock clock) {
        this.userStatsRepository = userStatsRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.userVocabularyRepository = userVocabularyRepository;
        this.dictationResultRepository = dictationResultRepository;
        this.userAchievementRepository = userAchievementRepository;
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
}