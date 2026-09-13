-- V2: Đổi cột timestamp sang TIMESTAMPTZ để khớp với Instant ở tầng Java
-- (TIMESTAMP không lưu offset — TIMESTAMPTZ mới đúng bản chất "1 điểm mốc UTC" của Instant)

ALTER TABLE users
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE refresh_tokens
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';