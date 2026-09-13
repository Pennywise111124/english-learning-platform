package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.englishlearningplatform.entity.UserProgress;

import java.util.List;
import java.util.Optional;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    // Tìm record hiện có cho 1 cặp (user, topic) — dùng để UPDATE TẠI CHỖ
    // (UserProgress là CURRENT STATE, không phải HISTORY — xem phân biệt ở mục 4
    // Requirements)
    Optional<UserProgress> findByUser_IdAndTopic_Id(Long userId, Long topicId);

    // GET /api/users/me/progress — trả TOÀN BỘ Topic User đã có hoạt động,
    // KHÔNG phân trang (khớp đúng shape mảng đã chốt ở FE_Handoff_Brief.md,
    // không phải PageResponse — khác với GET /api/topics)
    List<UserProgress> findByUser_Id(Long userId);

    // Chặn xoá Topic nếu đã có UserProgress — theo rule Content deletion mục 2.3
    boolean existsByTopic_Id(Long topicId);
}