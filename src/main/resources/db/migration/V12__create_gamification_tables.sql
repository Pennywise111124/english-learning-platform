-- V12: Gamification (XP + streak)

-- 1. Múi giờ của người dùng, dùng để tính "ngày địa phương" cho streak.
--    Tên IANA hợp lệ được kiểm ở tầng ứng dụng (ZoneId.of), DB chỉ chặn chuỗi rỗng.
ALTER TABLE users                                              -- CHECK TÊN BẢNG
    ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh';

ALTER TABLE users                                              -- CHECK TÊN BẢNG
    ADD CONSTRAINT ck_users_timezone_not_blank CHECK (length(trim(timezone)) > 0);

-- 2. Sổ cái XP: chỉ thêm, không sửa, không xoá.
--    Không cascade theo user: đây là lịch sử cá nhân (quy tắc mục 2.3).
CREATE TABLE xp_events (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    xp          INTEGER      NOT NULL,
    local_date  DATE         NOT NULL,
    dedupe_key  VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_xp_events_user FOREIGN KEY (user_id) REFERENCES users (id),  -- CHECK TÊN BẢNG
    CONSTRAINT ck_xp_events_type CHECK (type IN ('QUIZ_PARTICIPATION', 'QUIZ_PASS', 'VOCAB_REVIEW', 'DICTATION')),
    CONSTRAINT ck_xp_events_xp CHECK (xp >= 0),
    CONSTRAINT uq_xp_events_user_dedupe UNIQUE (user_id, dedupe_key)
);

-- Phục vụ thống kê theo ngày và lịch sử XP sau này
CREATE INDEX idx_xp_events_user_date ON xp_events (user_id, local_date DESC);

-- 3. Trạng thái tổng hợp hiện tại của mỗi người dùng (1 dòng / user).
CREATE TABLE user_stats (
    user_id           BIGINT      PRIMARY KEY,
    total_xp          BIGINT      NOT NULL DEFAULT 0,
    current_streak    INTEGER     NOT NULL DEFAULT 0,
    longest_streak    INTEGER     NOT NULL DEFAULT 0,
    last_active_date  DATE,
    freeze_used_week  DATE,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_user_stats_user FOREIGN KEY (user_id) REFERENCES users (id),  -- CHECK TÊN BẢNG
    CONSTRAINT ck_user_stats_non_negative CHECK (total_xp >= 0 AND current_streak >= 0 AND longest_streak >= 0),
    CONSTRAINT ck_user_stats_longest CHECK (longest_streak >= current_streak)
);

-- 4. Tạo sẵn dòng thống kê cho người dùng hiện có (bắt đầu từ 0, không backfill XP cũ).
INSERT INTO user_stats (user_id, updated_at)
SELECT id, NOW() FROM users;                                   -- CHECK TÊN BẢNG