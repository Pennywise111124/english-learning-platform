CREATE TABLE user_vocabulary (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users (id),
    flashcard_id     BIGINT      NOT NULL REFERENCES flashcards (id),
    status           VARCHAR(20) NOT NULL,
    review_count     INTEGER     NOT NULL DEFAULT 0,
    difficulty       INTEGER     NOT NULL DEFAULT 0,
    interval_level   INTEGER     NOT NULL DEFAULT 0,
    last_reviewed_at TIMESTAMPTZ,
    next_review_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_vocabulary_user_flashcard UNIQUE (user_id, flashcard_id)
);

-- Truy vấn chính của GET /today: lọc theo user rồi theo hạn ôn
CREATE INDEX idx_user_vocabulary_user_next_review ON user_vocabulary (user_id, next_review_at);