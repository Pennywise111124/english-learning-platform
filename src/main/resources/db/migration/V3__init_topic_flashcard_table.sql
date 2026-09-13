-- V3: Bảng Topic và Flashcard (FR-3)
-- Flashcard cascade delete theo Topic vì là nội dung Admin quản lý,
-- KHÔNG phải lịch sử cá nhân User (khác QuizAttempt/UserProgress — không cascade)

CREATE TABLE topics (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    level       VARCHAR(20)  NOT NULL,   -- lưu String theo EnumType.STRING đã chốt
    image_url   VARCHAR(500)
);

CREATE TABLE flashcards (
    id          BIGSERIAL PRIMARY KEY,
    topic_id    BIGINT       NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    word        VARCHAR(255) NOT NULL,
    meaning     VARCHAR(500) NOT NULL,
    example     TEXT,
    image_url   VARCHAR(500),
    audio_url   VARCHAR(500)             -- URL external do Admin tự nhập, không phải file hệ thống lưu (đã chốt mục 4)
);

-- Index cho FK, theo quy ước mục 2.3
CREATE INDEX idx_flashcards_topic_id ON flashcards(topic_id);

-- Chưa index title/level ở đây — để dành M10 khi làm Search/Filter thật (FR-9),
-- tránh over-index khi chưa có nhu cầu truy vấn thực tế