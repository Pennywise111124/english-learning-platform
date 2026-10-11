package com.example.englishlearningplatform.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.englishlearningplatform.entity.AchievementCode;
import com.example.englishlearningplatform.entity.UserAchievement;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserId(Long userId);

    @Modifying
    @Query(value = "INSERT INTO user_achievements (user_id, code, awarded_at) "
            + "VALUES (:userId, :code, :awardedAt) "
            + "ON CONFLICT (user_id, code) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId,
            @Param("code") String code,
            @Param("awardedAt") Instant awardedAt);

    @Modifying
    @Query("UPDATE UserAchievement a SET a.dismissedAt = :now "
            + "WHERE a.userId = :userId AND a.code = :code AND a.dismissedAt IS NULL")
    int dismiss(@Param("userId") Long userId,
            @Param("code") AchievementCode code,
            @Param("now") Instant now);
}