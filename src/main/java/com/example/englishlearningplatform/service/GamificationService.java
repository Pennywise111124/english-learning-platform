package com.example.englishlearningplatform.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.englishlearningplatform.config.GamificationProperties;
import com.example.englishlearningplatform.dto.stats.UserStatsResponse;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.entity.UserStats;
import com.example.englishlearningplatform.entity.XpType;
import com.example.englishlearningplatform.event.LearningActivityEvent;
import com.example.englishlearningplatform.exception.ResourceNotFoundException;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.repository.UserStatsRepository;
import com.example.englishlearningplatform.repository.XpEventRepository;
import com.example.englishlearningplatform.service.StreakCalculator.State;
import com.example.englishlearningplatform.util.TimezoneUtils;

@Service
public class GamificationService {

    private static final Logger log = LoggerFactory.getLogger(GamificationService.class);

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final XpEventRepository xpEventRepository;
    private final GamificationProperties props;
    private final Clock clock;

    public GamificationService(UserRepository userRepository,
            UserStatsRepository userStatsRepository,
            XpEventRepository xpEventRepository,
            GamificationProperties props,
            Clock clock) {
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
        this.xpEventRepository = xpEventRepository;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(LearningActivityEvent e) {
        Optional<User> found = userRepository.findById(e.userId());
        if (found.isEmpty()) {
            log.warn("Gamification: user {} not found, skipping {}", e.userId(), e.kind());
            return;
        }
        User user = found.get();

        LocalDate day = e.occurredAt().atZone(zoneOf(user)).toLocalDate();
        Instant now = clock.instant();

        userStatsRepository.insertIfAbsent(user.getId(), now);
        UserStats stats = userStatsRepository.findByUserIdForUpdate(user.getId()).orElseThrow();

        int gained = awardXp(e, day, now);
        stats.setTotalXp(stats.getTotalXp() + gained);

        State next = StreakCalculator.onActivity(toState(stats), day);
        stats.setCurrentStreak(next.currentStreak());
        stats.setLongestStreak(next.longestStreak());
        stats.setLastActiveDate(next.lastActiveDate());
        stats.setFreezeUsedWeek(next.freezeUsedWeek());
        stats.setUpdatedAt(now);

        userStatsRepository.save(stats);
    }

    private int awardXp(LearningActivityEvent e, LocalDate day, Instant now) {
        Long userId = e.userId();
        Long ref = e.refId();
        int total = 0;

        switch (e.kind()) {
            case QUIZ_SUBMITTED -> {
                for (int slot = 1; slot <= props.quizParticipationDailyLimit(); slot++) {
                    String key = "QP:" + ref + ":" + day + ":" + slot;
                    if (grant(userId, XpType.QUIZ_PARTICIPATION, props.xpQuizParticipation(), day, key, now)) {
                        total += props.xpQuizParticipation();
                        break;
                    }
                }
                if (e.metric() >= props.quizPassScore()
                        && grant(userId, XpType.QUIZ_PASS, props.xpQuizPass(), day, "QPASS:" + ref, now)) {
                    total += props.xpQuizPass();
                }
            }
            case VOCAB_REVIEWED -> {
                if (e.metric() == 1
                        && grant(userId, XpType.VOCAB_REVIEW, props.xpVocabReview(), day,
                                "VR:" + ref + ":" + day, now)) {
                    total += props.xpVocabReview();
                }
            }
            case DICTATION_SUBMITTED -> {
                int xp = (int) Math.min(props.xpDictationMax(), Math.round(e.metric() / 10.0));
                if (xp > 0 && grant(userId, XpType.DICTATION, xp, day, "DC:" + ref + ":" + day, now)) {
                    total += xp;
                }
            }
        }
        return total;
    }

    private boolean grant(Long userId, XpType type, int xp, LocalDate day, String key, Instant now) {
        if (xp <= 0) {
            return false;
        }
        return xpEventRepository.insertIfAbsent(userId, type.name(), xp, day, key, now) == 1;
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        LocalDate today = LocalDate.now(clock.withZone(zoneOf(user)));

        UserStats stats = userStatsRepository.findById(user.getId()).orElse(null);
        if (stats == null) {
            return new UserStatsResponse(0, LevelCalculator.level(0), LevelCalculator.xpToNextLevel(0),
                    0, 0, false, true);
        }

        State s = toState(stats);
        return new UserStatsResponse(
                stats.getTotalXp(),
                LevelCalculator.level(stats.getTotalXp()),
                LevelCalculator.xpToNextLevel(stats.getTotalXp()),
                StreakCalculator.displayStreak(s, today),
                stats.getLongestStreak(),
                today.equals(stats.getLastActiveDate()),
                StreakCalculator.freezeAvailable(s, today));
    }

    private static State toState(UserStats stats) {
        return new State(stats.getCurrentStreak(), stats.getLongestStreak(),
                stats.getLastActiveDate(), stats.getFreezeUsedWeek());
    }

    private ZoneId zoneOf(User user) {
        try {
            return ZoneId.of(user.getTimezone());
        } catch (DateTimeException ex) {
            log.warn("User {} has invalid timezone '{}', using default", user.getId(), user.getTimezone());
            return ZoneId.of(TimezoneUtils.DEFAULT);
        }
    }
}