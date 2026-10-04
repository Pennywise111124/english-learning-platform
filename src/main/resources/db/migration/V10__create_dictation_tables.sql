-- V10: Dictation (FR-7)
-- dictation_lessons: nội dung Admin quản lý -> cascade theo topics (giống flashcards ở V3)
-- dictation_results: lịch sử cá nhân User -> KHÔNG cascade (giống user_vocabulary ở V9)

CREATE TABLE dictation_lessons (
    id         BIGSERIAL PRIMARY KEY,
    topic_id   BIGINT       NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    media_url  VARCHAR(500),               -- NULL cho đến khi Admin upload audio
    transcript TEXT         NOT NULL,
    level      VARCHAR(20)  NOT NULL
);
CREATE INDEX idx_dictation_lessons_topic_id ON dictation_lessons (topic_id);

CREATE TABLE dictation_results (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT           NOT NULL REFERENCES users (id),
    lesson_id  BIGINT           NOT NULL REFERENCES dictation_lessons (id),
    user_input TEXT             NOT NULL,
    accuracy   DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ      NOT NULL
);
-- GET /results: lọc theo (user, lesson), sắp xếp mới nhất trước
CREATE INDEX idx_dictation_results_user_lesson_created
    ON dictation_results (user_id, lesson_id, created_at DESC);
-- existsByLesson_Id / existsByLesson_Topic_Id khi Admin xoá Lesson/Topic
CREATE INDEX idx_dictation_results_lesson_id ON dictation_results (lesson_id);