package com.example.englishlearningplatform.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.englishlearningplatform.entity.UserStats;

import jakarta.persistence.LockModeType;

public interface UserStatsRepository extends JpaRepository<UserStats, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserStats s where s.userId = :userId")
    Optional<UserStats> findByUserIdForUpdate(@Param("userId") Long userId);

    @Modifying
    @Query(value = "INSERT INTO user_stats (user_id, updated_at) VALUES (:userId, :now) "
            + "ON CONFLICT (user_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("now") Instant now);
}