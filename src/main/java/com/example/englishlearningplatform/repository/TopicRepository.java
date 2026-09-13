package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    // M2: chỉ cần findAll(Pageable) có sẵn từ JpaRepository — đủ cho pagination cơ
    // bản.
    // TODO (M10): thêm method/@Query cho search theo keyword (title/description)
    // + filter theo level + sort theo mới nhất/phổ biến (FR-9).

    // Chặn tạo 2 Topic trùng cả title lẫn level — khớp UNIQUE constraint ở V4.
    // TopicService.create() cần gọi method này TRƯỚC khi save, để trả lỗi rõ ràng
    // (400/409) thay vì để DB tự ném DataIntegrityViolationException khó xử lý đẹp.
    boolean existsByTitleAndLevel(String title, Level level);

    boolean existsByTitleAndLevelAndIdNot(String title, Level level, Long id);
}