CREATE TABLE conversations (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    title       VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

-- Phục vụ GET /api/conversations?page=&size= (WHERE user_id = ? ORDER BY updated_at DESC)
CREATE INDEX idx_conversations_user_updated_at ON conversations (user_id, updated_at DESC);

CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT      NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender          VARCHAR(10) NOT NULL,
    content         TEXT        NOT NULL,
    correction      TEXT,
    explanation     TEXT,
    created_at      TIMESTAMPTZ NOT NULL
);

-- Phục vụ GET /api/conversations/{id}/messages (WHERE conversation_id = ? ORDER BY created_at ASC)
CREATE INDEX idx_messages_conversation_created_at ON messages (conversation_id, created_at ASC);