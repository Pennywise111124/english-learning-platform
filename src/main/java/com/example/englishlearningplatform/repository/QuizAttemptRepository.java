package com.example.englishlearningplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.englishlearningplatform.entity.QuizAttempt;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    // GET /api/quizzes/{id}/attempts — lịch sử làm bài của chính User cho 1 Quiz cụ
    // thể
    Page<QuizAttempt> findByUser_IdAndQuiz_Id(Long userId, Long quizId, Pageable pageable);

    // Chặn xoá Quiz nếu đã có người làm — theo rule Content deletion mục 2.3
    boolean existsByQuiz_Id(Long quizId);

    // Chặn xoá Topic nếu BẤT KỲ Quiz con nào của Topic đã có người làm
    // (xuyên qua 2 cấp quan hệ: QuizAttempt -> Quiz -> Topic)
    boolean existsByQuiz_Topic_Id(Long topicId);

    /**
     * Đếm số Quiz (distinct) User đã "đạt" (score >= minScore) trong 1 Topic —
     * dùng cho công thức progressPercent đã chốt. Quá phức tạp để Spring Data
     * tự derive từ tên method (cần DISTINCT + so sánh >= cùng lúc), nên viết
     * 
     * @Query JPQL trực tiếp.
     */
    @Query("SELECT COUNT(DISTINCT qa.quiz.id) FROM QuizAttempt qa " +
            "WHERE qa.user.id = :userId AND qa.quiz.topic.id = :topicId AND qa.score >= :minScore")
    long countAchievedQuizzesInTopic(@Param("userId") Long userId,
            @Param("topicId") Long topicId,
            @Param("minScore") int minScore);
}