package com.example.englishlearningplatform.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.example.englishlearningplatform.dto.leaderboard.LeaderboardEntryResponse;
import com.example.englishlearningplatform.dto.leaderboard.LeaderboardResponse;
import com.example.englishlearningplatform.entity.User;
import com.example.englishlearningplatform.repository.PostgresIntegrationTestBase;
import com.example.englishlearningplatform.repository.UserRepository;
import com.example.englishlearningplatform.repository.XpEventRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LeaderboardService.class)
class LeaderboardServiceTest extends PostgresIntegrationTestBase {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock leaderboardTestClock() {
            return Clock.fixed(Instant.parse("2026-10-09T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    private static final LocalDate MON = LocalDate.of(2026, 10, 5);
    private static final LocalDate FRI = LocalDate.of(2026, 10, 9);
    private static final LocalDate SUN = LocalDate.of(2026, 10, 11);
    private static final Instant CREATED = Instant.parse("2026-10-09T10:00:00Z");
    private static final Instant NEAR_BOUNDARY = Instant.parse("2026-10-11T20:00:00Z");

    @Autowired
    LeaderboardService service;
    @Autowired
    XpEventRepository xpEventRepository;
    @Autowired
    EntityManager em;
    @Autowired
    UserRepository userRepository;

    private final AtomicInteger seq = new AtomicInteger();

    // ---------- helpers ----------

    private User user(String prefix, boolean visible) {
        User u = new User();
        u.setUsername(prefix + "-" + System.nanoTime() + "-" + seq.incrementAndGet());
        u.setEmail(u.getUsername() + "@test.local");
        u.setPasswordHash("hash");
        u.setLeaderboardVisible(visible);
        em.persist(u);
        em.flush();
        return u;
    }

    private void xp(User u, int amount, LocalDate day) {
        xpEventRepository.insertIfAbsent(u.getId(), "VOCAB_REVIEW", amount, day,
                "t:" + seq.incrementAndGet(), CREATED);
    }

    private LeaderboardResponse weeklyFor(User viewer) {
        em.flush();
        em.clear();
        return service.getWeekly(viewer.getUsername());
    }

    private User userInZone(String prefix, String timezone) {
        User u = user(prefix, true);
        u.setTimezone(timezone);
        em.flush();
        return u;
    }

    private LeaderboardService serviceAt(Instant instant) {
        return new LeaderboardService(userRepository, xpEventRepository,
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    private LeaderboardResponse weeklyAt(Instant instant, User viewer) {
        em.flush();
        em.clear();
        return serviceAt(instant).getWeekly(viewer.getUsername());
    }

    private void forceHashAggregation() {
        em.createNativeQuery("SET LOCAL enable_sort = off").executeUpdate();
        em.createNativeQuery("SET LOCAL enable_indexscan = off").executeUpdate();
        em.createNativeQuery("SET LOCAL enable_indexonlyscan = off").executeUpdate();
    }

    // ---------- tests ----------

    @Test
    void entriesAreOrderedByXpDescending_withRanks123() {
        User a = user("a", true);
        User b = user("b", true);
        User c = user("c", true);
        xp(a, 10, FRI);
        xp(b, 30, FRI);
        xp(c, 20, FRI);

        LeaderboardResponse r = weeklyFor(a);

        assertThat(r.weekStart()).isEqualTo(MON);
        assertThat(r.weekEnd()).isEqualTo(SUN);
        assertThat(r.entries()).extracting(LeaderboardEntryResponse::rank).containsExactly(1, 2, 3);
        assertThat(r.entries()).extracting(LeaderboardEntryResponse::username)
                .containsExactly(b.getUsername(), c.getUsername(), a.getUsername());
        assertThat(r.entries()).extracting(LeaderboardEntryResponse::xp).containsExactly(30L, 20L, 10L);
    }

    @Test
    void tiedXp_lowerUserIdRanksFirst_andMeRankAgrees() {
        User first = user("tie1", true);
        User second = user("tie2", true);
        xp(first, 50, FRI);
        xp(second, 50, FRI);

        LeaderboardResponse r = weeklyFor(second);

        assertThat(r.entries()).extracting(LeaderboardEntryResponse::username)
                .containsExactly(first.getUsername(), second.getUsername());
        assertThat(r.me().rank()).isEqualTo(2);
    }

    @Test
    void hiddenUser_isNotListed_andDoesNotTakeRankFromOthers() {
        User hidden = user("hid", false);
        User low = user("low", true);
        xp(hidden, 100, FRI);
        xp(low, 10, FRI);

        LeaderboardResponse r = weeklyFor(low);

        assertThat(r.entries()).extracting(LeaderboardEntryResponse::username)
                .containsExactly(low.getUsername());
        assertThat(r.entries().get(0).rank()).isEqualTo(1);
        assertThat(r.me().rank()).isEqualTo(1);
    }

    @Test
    void onlyEventsInsideMondayToSundayCount() {
        User u = user("range", true);
        xp(u, 1, LocalDate.of(2026, 10, 4));
        xp(u, 10, MON);
        xp(u, 100, SUN);
        xp(u, 1000, LocalDate.of(2026, 10, 12));
        LeaderboardResponse r = weeklyFor(u);

        assertThat(r.entries()).hasSize(1);
        assertThat(r.entries().get(0).xp()).isEqualTo(110L);
        assertThat(r.me().xp()).isEqualTo(110L);
    }

    @Test
    void moreThanTwentyUsers_entriesCappedAtTwenty_viewerRank21ReportedInMe() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            User u = user("n" + i, true);
            xp(u, 1000 - i * 10, FRI);
            users.add(u);
        }
        User viewer = users.get(20);

        LeaderboardResponse r = weeklyFor(viewer);

        assertThat(r.entries()).hasSize(20);
        assertThat(r.entries()).extracting(LeaderboardEntryResponse::username)
                .doesNotContain(viewer.getUsername());
        assertThat(r.me().rank()).isEqualTo(21);
        assertThat(r.me().xp()).isEqualTo(800L);
    }

    @Test
    void viewerWithoutXpThisWeek_hasNullRankAndZeroXp() {
        User active = user("act", true);
        User idle = user("idle", true);
        xp(active, 10, FRI);
        xp(idle, 5, LocalDate.of(2026, 10, 4));

        LeaderboardResponse r = weeklyFor(idle);

        assertThat(r.me().rank()).isNull();
        assertThat(r.me().xp()).isEqualTo(0L);
    }

    @Test
    void hiddenViewer_stillGetsImaginaryRank_withVisibleFalse() {
        User top = user("top", true);
        User hidden = user("hv", false);
        xp(top, 50, FRI);
        xp(hidden, 30, FRI);

        LeaderboardResponse r = weeklyFor(hidden);

        assertThat(r.me().rank()).isEqualTo(2);
        assertThat(r.me().visible()).isFalse();
        assertThat(r.entries()).extracting(LeaderboardEntryResponse::username)
                .containsExactly(top.getUsername());
    }

    @Test
    void meFlagIsTrueExactlyForTheViewer() {
        User a = user("ma", true);
        User b = user("mb", true);
        User c = user("mc", true);
        xp(a, 30, FRI);
        xp(b, 20, FRI);
        xp(c, 10, FRI);

        LeaderboardResponse r = weeklyFor(b);

        assertThat(r.entries()).filteredOn(LeaderboardEntryResponse::me)
                .extracting(LeaderboardEntryResponse::username)
                .containsExactly(b.getUsername());
    }

    @Test
    void viewerInHanoi_afterLocalMidnight_seesNewWeek() {
        User hanoi = userInZone("hn", "Asia/Ho_Chi_Minh");
        xp(hanoi, 100, SUN);
        xp(hanoi, 5, LocalDate.of(2026, 10, 12));

        LeaderboardResponse r = weeklyAt(NEAR_BOUNDARY, hanoi);

        assertThat(r.weekStart()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(r.weekEnd()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(r.me().xp()).isEqualTo(5L);
        assertThat(r.entries()).extracting(LeaderboardEntryResponse::xp).containsExactly(5L);
    }

    @Test
    void viewerInLosAngeles_atSameInstant_stillSeesOldWeek() {
        User la = userInZone("la", "America/Los_Angeles");
        xp(la, 100, SUN);
        xp(la, 5, LocalDate.of(2026, 10, 12));

        LeaderboardResponse r = weeklyAt(NEAR_BOUNDARY, la);

        assertThat(r.weekStart()).isEqualTo(MON);
        assertThat(r.weekEnd()).isEqualTo(SUN);
        assertThat(r.me().xp()).isEqualTo(100L);
        assertThat(r.entries()).extracting(LeaderboardEntryResponse::xp).containsExactly(100L);
    }

    @Test
    void viewerWithInvalidTimezone_fallsBackToDefaultZoneWeek() {
        User broken = userInZone("bad", "Mars/Base");
        xp(broken, 100, SUN);
        xp(broken, 5, LocalDate.of(2026, 10, 12));

        LeaderboardResponse r = weeklyAt(NEAR_BOUNDARY, broken);

        assertThat(r.weekStart()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(r.me().xp()).isEqualTo(5L);
    }

    @Test
    void tiedXp_manyUsers_orderedByUserId_evenWhenPlannerUsesHashAggregate() {
        int tiedUsers = 12;
        List<User> users = new ArrayList<>();
        for (int i = 0; i < tiedUsers; i++) {
            users.add(user("hh" + i, true));
        }
        for (int i = tiedUsers - 1; i >= 0; i--) {
            xp(users.get(i), 50, FRI);
        }
        em.flush();
        forceHashAggregation();

        LeaderboardResponse r = weeklyFor(users.get(0));

        assertThat(r.entries()).extracting(LeaderboardEntryResponse::username)
                .containsExactlyElementsOf(users.stream().map(User::getUsername).toList());
        assertThat(r.entries()).extracting(LeaderboardEntryResponse::rank)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }
}