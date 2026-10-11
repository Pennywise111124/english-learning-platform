package com.example.englishlearningplatform.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}