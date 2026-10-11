package com.example.englishlearningplatform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.englishlearningplatform.dto.stats.UserStatsResponse;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.entity.UserStats;
import com.example.englishlearningplatform.entity.XpEvent;
import com.example.englishlearningplatform.entity.XpType;
import com.example.englishlearningplatform.event.LearningActivityEvent;
import com.example.englishlearningplatform.repository.PostgresIntegrationTestBase;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.repository.UserStatsRepository;
import com.example.englishlearningplatform.repository.XpEventRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ GamificationService.class, GamificationTestConfig.class })
@TestPropertySource(properties = {
        "app.gamification.xp-quiz-participation=10",
        "app.gamification.xp-quiz-pass=10",
        "app.gamification.xp-vocab-review=2",
        "app.gamification.xp-dictation-max=10",
        "app.gamification.quiz-pass-score=70",
        "app.gamification.quiz-participation-daily-limit=3" })
@Transactional(propagation = Propagation.NOT_SUPPORTED) // commit thật: record() chạy REQUIRES_NEW
class GamificationServiceTest extends PostgresIntegrationTestBase {

    private static final String HCM = "Asia/Ho_Chi_Minh";
    private static final String MORNING_OCT_9 = "2026-10-09T03:00:00Z"; // 10:00 ngày 9/10 ở Hà Nội

    @Autowired
    GamificationService service;
    @Autowired
    UserRepository userRepo;
    @Autowired
    UserStatsRepository statsRepo;
    @Autowired
    XpEventRepository xpRepo;

    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : createdUserIds) {
            xpRepo.deleteAll(xpRepo.findByUserId(id));
            statsRepo.deleteById(id);
            userRepo.deleteById(id);
        }
    }

    // ═══════════ Helpers ═══════════

    private User newUser(String timezone) {
        User u = new User();
        u.setUsername("gm-" + UUID.randomUUID().toString().substring(0, 8));
        u.setEmail(UUID.randomUUID() + "@test.local");
        u.setPasswordHash("hash");
        u.setTimezone(timezone);
        User saved = userRepo.save(u);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private static LearningActivityEvent quiz(User u, long quizId, int score, String iso) {
        return LearningActivityEvent.quizSubmitted(u.getId(), quizId, score, Instant.parse(iso));
    }

    private static LearningActivityEvent vocab(User u, long id, boolean remembered, String iso) {
        return LearningActivityEvent.vocabReviewed(u.getId(), id, remembered, Instant.parse(iso));
    }

    private static LearningActivityEvent dictation(User u, long lessonId, double accuracy, String iso) {
        return LearningActivityEvent.dictationSubmitted(u.getId(), lessonId, accuracy, Instant.parse(iso));
    }

    private UserStats stats(User u) {
        return statsRepo.findById(u.getId()).orElseThrow();
    }

    private List<XpEvent> ledger(User u) {
        return xpRepo.findByUserId(u.getId());
    }

    // ═══════════ Quiz ═══════════

    @Test
    void quizFourthTimeSameDay_givesNoMoreXp() {
        User u = newUser(HCM);

        for (int i = 0; i < 4; i++) {
            service.record(quiz(u, 7L, 50, MORNING_OCT_9));
        }

        assertThat(stats(u).getTotalXp()).isEqualTo(30); // 3 lần x 10, lần 4 = 0
        assertThat(ledger(u)).hasSize(3).allMatch(e -> e.getType() == XpType.QUIZ_PARTICIPATION);
        assertThat(stats(u).getCurrentStreak()).isEqualTo(1);
    }

    @Test
    void quizPass_isAwardedOnlyOncePerQuiz() {
        User u = newUser(HCM);

        service.record(quiz(u, 7L, 80, "2026-10-09T03:00:00Z")); // 10 tham gia + 10 đạt
        service.record(quiz(u, 7L, 90, "2026-10-10T03:00:00Z")); // 10 tham gia, đạt đã nhận

        assertThat(stats(u).getTotalXp()).isEqualTo(30);
        assertThat(ledger(u)).filteredOn(e -> e.getType() == XpType.QUIZ_PASS).hasSize(1);
        assertThat(stats(u).getCurrentStreak()).isEqualTo(2);
    }

    @Test
    void quizPassThreshold_isInclusiveAt70() {
        User u = newUser(HCM);

        service.record(quiz(u, 1L, 69, MORNING_OCT_9)); // chỉ tham gia
        service.record(quiz(u, 2L, 70, MORNING_OCT_9)); // tham gia + đạt

        assertThat(ledger(u)).filteredOn(e -> e.getType() == XpType.QUIZ_PASS)
                .extracting(XpEvent::getDedupeKey).containsExactly("QPASS:2");
        assertThat(stats(u).getTotalXp()).isEqualTo(30);
    }

    // ═══════════ Dictation ═══════════

    @Test
    void dictationWithZeroXp_doesNotTakeTheKey_soBetterAttemptStillCounts() {
        User u = newUser(HCM);

        service.record(dictation(u, 4L, 3.0, MORNING_OCT_9)); // round(0.3) = 0: không ghi sổ cái
        assertThat(ledger(u)).isEmpty();
        assertThat(stats(u).getTotalXp()).isZero();
        assertThat(stats(u).getCurrentStreak()).isEqualTo(1);

        service.record(dictation(u, 4L, 85.0, MORNING_OCT_9)); // round(8.5) = 9
        service.record(dictation(u, 4L, 100.0, MORNING_OCT_9)); // cùng bài, cùng ngày: đã chiếm khoá

        assertThat(stats(u).getTotalXp()).isEqualTo(9);
        assertThat(ledger(u)).singleElement().satisfies(e -> {
            assertThat(e.getXp()).isEqualTo(9);
            assertThat(e.getDedupeKey()).isEqualTo("DC:4:2026-10-09");
        });
    }

    @Test
    void dictationXp_isCappedAtMax() {
        User u = newUser(HCM);

        service.record(dictation(u, 5L, 100.0, MORNING_OCT_9));

        assertThat(stats(u).getTotalXp()).isEqualTo(10);
    }

    // ═══════════ Streak không phụ thuộc XP ═══════════

    @Test
    void zeroXpActivities_stillKeepStreakGoing() {
        User u = newUser(HCM);

        service.record(quiz(u, 7L, 50, "2026-10-08T03:00:00Z")); // 8/10: +10 XP
        service.record(dictation(u, 3L, 2.0, "2026-10-09T03:00:00Z")); // 9/10: 0 XP
        service.record(vocab(u, 5L, false, "2026-10-10T03:00:00Z")); // 10/10: "chưa nhớ" 0 XP

        UserStats s = stats(u);
        assertThat(s.getTotalXp()).isEqualTo(10);
        assertThat(ledger(u)).hasSize(1);
        assertThat(s.getCurrentStreak()).isEqualTo(3);
        assertThat(s.getLastActiveDate()).isEqualTo(LocalDate.of(2026, 10, 10));
    }

    // ═══════════ Múi giờ ═══════════

    @Test
    void sameVietnamDay_acrossUtcMidnight_countsOnce() {
        User u = newUser("Asia/Ho_Chi_Minh");

        // 06:30 và 07:30 ngày 10/10 giờ Hà Nội, nhưng khác ngày theo UTC
        service.record(vocab(u, 1L, true, "2026-10-09T23:30:00Z"));
        service.record(vocab(u, 1L, true, "2026-10-10T00:30:00Z"));

        UserStats s = stats(u);
        assertThat(s.getTotalXp()).isEqualTo(2);
        assertThat(s.getCurrentStreak()).isEqualTo(1);
        assertThat(s.getLastActiveDate()).isEqualTo(LocalDate.of(2026, 10, 10));
    }

    @Test
    void differentLosAngelesDays_withinOneUtcDay_countTwice() {
        User u = newUser("America/Los_Angeles");

        // Cùng ngày 9/10 theo UTC, nhưng 23:30 ngày 8/10 và 00:30 ngày 9/10 ở Los
        // Angeles (UTC-7)
        service.record(vocab(u, 1L, true, "2026-10-09T06:30:00Z"));
        service.record(vocab(u, 1L, true, "2026-10-09T07:30:00Z"));

        UserStats s = stats(u);
        assertThat(s.getTotalXp()).isEqualTo(4);
        assertThat(s.getCurrentStreak()).isEqualTo(2);
        assertThat(s.getLastActiveDate()).isEqualTo(LocalDate.of(2026, 10, 9));
    }

    // ═══════════ User chưa có dòng stats / không tồn tại ═══════════

    @Test
    void userWithoutStatsRow_getsRowCreatedOnFirstActivity() {
        User u = newUser(HCM);
        assertThat(statsRepo.findById(u.getId())).isEmpty();

        service.record(vocab(u, 1L, true, MORNING_OCT_9));

        assertThat(stats(u).getTotalXp()).isEqualTo(2);
    }

    @Test
    void unknownUser_isSkippedWithoutError() {
        LearningActivityEvent e = LearningActivityEvent.vocabReviewed(
                999_999_999L, 1L, true, Instant.parse(MORNING_OCT_9));

        assertThatCode(() -> service.record(e)).doesNotThrowAnyException();
        assertThat(statsRepo.findById(999_999_999L)).isEmpty();
    }

    // ═══════════ getStats (Clock cố định = 2026-10-09T10:00Z, tức 17:00 ngày 9/10
    // ở Hà Nội) ═══════════

    @Test
    void getStats_userWithoutStatsRow_returnsZeros() {
        User u = newUser(HCM);

        UserStatsResponse r = service.getStats(u.getUsername());

        assertThat(r).isEqualTo(new UserStatsResponse(0, 1, 100, 0, 0, false, true));
    }

    @Test
    void getStats_afterActivityToday_reportsActiveToday() {
        User u = newUser(HCM);
        service.record(quiz(u, 7L, 50, "2026-10-09T09:00:00Z")); // 16:00 ngày 9/10: +10 XP

        UserStatsResponse r = service.getStats(u.getUsername());

        assertThat(r).isEqualTo(new UserStatsResponse(10, 1, 90, 1, 1, true, true));
    }

    @Test
    void getStats_lastActiveYesterday_keepsStreakButNotActiveToday() {
        User u = newUser(HCM);
        service.record(quiz(u, 7L, 50, "2026-10-08T09:00:00Z"));

        UserStatsResponse r = service.getStats(u.getUsername());

        assertThat(r.currentStreak()).isEqualTo(1);
        assertThat(r.activeToday()).isFalse();
    }

    @Test
    void getStats_staleStreak_displaysZeroButKeepsLongest() {
        User u = newUser(HCM);
        service.record(quiz(u, 7L, 50, "2026-10-05T09:00:00Z")); // 5/10, cách hôm nay 4 ngày

        UserStatsResponse r = service.getStats(u.getUsername());

        assertThat(r).isEqualTo(new UserStatsResponse(10, 1, 90, 0, 1, false, true));
    }

    @Test
    void getStats_afterFreezeUsedThisWeek_reportsFreezeUnavailableAndKeepsStreak() {
        User u = newUser(HCM);
        service.record(quiz(u, 7L, 50, "2026-10-07T09:00:00Z")); // T4 7/10
        service.record(quiz(u, 7L, 50, "2026-10-09T03:00:00Z")); // T6 9/10 (bỏ lỡ 8/10, đóng băng cứu)

        UserStatsResponse r = service.getStats(u.getUsername());

        assertThat(r).isEqualTo(new UserStatsResponse(20, 1, 80, 2, 2, true, false));
    }
}