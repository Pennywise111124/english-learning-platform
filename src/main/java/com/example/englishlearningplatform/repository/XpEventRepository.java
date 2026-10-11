package com.example.englishlearningplatform.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.englishlearningplatform.dto.leaderboard.LeaderboardRow;
import com.example.englishlearningplatform.entity.XpEvent;

public interface XpEventRepository extends JpaRepository<XpEvent, Long> {

        List<XpEvent> findByUserId(Long userId);

        @Modifying
        @Query(value = "INSERT INTO xp_events (user_id, type, xp, local_date, dedupe_key, created_at) "
                        + "VALUES (:userId, :type, :xp, :localDate, :dedupeKey, :createdAt) "
                        + "ON CONFLICT (user_id, dedupe_key) DO NOTHING", nativeQuery = true)
        int insertIfAbsent(@Param("userId") Long userId, @Param("type") String type, @Param("xp") int xp,
                        @Param("localDate") LocalDate localDate, @Param("dedupeKey") String dedupeKey,
                        @Param("createdAt") Instant createdAt);

        @Query(value = "SELECT e.user_id AS \"userId\", u.username AS \"username\", "
                        + "CAST(SUM(e.xp) AS BIGINT) AS \"xp\" "
                        + "FROM xp_events e JOIN users u ON u.id = e.user_id "
                        + "WHERE e.local_date BETWEEN :from AND :to AND u.leaderboard_visible = TRUE "
                        + "GROUP BY e.user_id, u.username "
                        + "ORDER BY SUM(e.xp) DESC, e.user_id ASC "
                        + "LIMIT :limit", nativeQuery = true)
        List<LeaderboardRow> findWeeklyTop(@Param("from") LocalDate from, @Param("to") LocalDate to,
                        @Param("limit") int limit);

        @Query(value = "SELECT COALESCE(SUM(xp), 0) FROM xp_events "
                        + "WHERE user_id = :userId AND local_date BETWEEN :from AND :to", nativeQuery = true)
        long sumXpBetween(@Param("userId") Long userId, @Param("from") LocalDate from,
                        @Param("to") LocalDate to);

        @Query(value = "SELECT COUNT(*) FROM ("
                        + "SELECT e.user_id FROM xp_events e JOIN users u ON u.id = e.user_id "
                        + "WHERE e.local_date BETWEEN :from AND :to AND u.leaderboard_visible = TRUE "
                        + "GROUP BY e.user_id "
                        + "HAVING SUM(e.xp) > :myXp OR (SUM(e.xp) = :myXp AND e.user_id < :userId)"
                        + ") t", nativeQuery = true)
        long countRankedAhead(@Param("userId") Long userId, @Param("myXp") long myXp,
                        @Param("from") LocalDate from, @Param("to") LocalDate to);
}