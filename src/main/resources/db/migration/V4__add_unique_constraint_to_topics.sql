-- V4: Ràng buộc UNIQUE(title, level) cho Topic
-- Cho phép trùng title nếu khác level (VD: "Travel" - BEGINNER và "Travel" - ADVANCED
-- là 2 Topic hợp lệ), nhưng chặn trùng cả 2 cùng lúc

ALTER TABLE topics
    ADD CONSTRAINT uq_topics_title_level UNIQUE (title, level);