-- V13: Leaderboard + Achievement

-- 1. Quyền riêng tư bảng xếp hạng
ALTER TABLE users
    ADD COLUMN leaderboard_visible BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Bảng xếp hạng tuần lọc theo local_date trên MỌI user,
--    index cũ (user_id, local_date) không phục vụ được truy vấn này.
CREATE INDEX idx_xp_events_date_user ON xp_events (local_date, user_id);

-- 3. Achievement. Cố ý KHÔNG có CHECK cho code (danh sách sẽ tăng thường xuyên).
CREATE TABLE user_achievements (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    code          VARCHAR(40) NOT NULL,
    awarded_at    TIMESTAMPTZ NOT NULL,
    dismissed_at  TIMESTAMPTZ,
    CONSTRAINT fk_user_achievements_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_user_achievements_user_code UNIQUE (user_id, code)
);

CREATE INDEX idx_user_achievements_pending
    ON user_achievements (user_id) WHERE dismissed_at IS NULL;