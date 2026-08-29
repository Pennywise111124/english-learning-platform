-- M1: Bảng User và Refresh Token
-- Đặt trong src/main/resources/db/migration/ để Flyway tự chạy khi app khởi động

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',   -- lưu String theo EnumType.STRING đã chốt trong Requirements
    avatar_url    VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- Lưu Refresh Token trong DB để có thể thu hồi (revoke) khi cần,
-- thay vì chỉ dựa vào thời gian hết hạn của token
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
