package com.example.englishlearningplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;

public interface TopicRepository extends JpaRepository<Topic, Long>, JpaSpecificationExecutor<Topic> {

    // M2: chỉ cần findAll(Pageable) có sẵn từ JpaRepository — đủ cho pagination cơ
    // bản.

    // Chặn tạo 2 Topic trùng cả title lẫn level — khớp UNIQUE constraint ở V4.
    // TopicService.create() cần gọi method này TRƯỚC khi save, để trả lỗi rõ ràng
    // (400/409) thay vì để DB tự ném DataIntegrityViolationException khó xử lý đẹp.
    boolean existsByTitleAndLevel(String title, Level level);

    boolean existsByTitleAndLevelAndIdNot(String title, Level level, Long id);

}