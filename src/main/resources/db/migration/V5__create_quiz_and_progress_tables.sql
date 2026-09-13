-- V5: Bảng Quiz, QuizQuestion (+ bảng phụ options), QuizAttempt, UserProgress (FR-4, FR-5)

CREATE TABLE quizzes (
    id       BIGSERIAL PRIMARY KEY,
    topic_id BIGINT       NOT NULL REFERENCES topics(id) ON DELETE CASCADE,  -- nội dung Admin, giống Flashcard
    title    VARCHAR(255) NOT NULL
);
CREATE INDEX idx_quizzes_topic_id ON quizzes(topic_id);

CREATE TABLE quiz_questions (
    id             BIGSERIAL PRIMARY KEY,
    quiz_id        BIGINT NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE, -- nội dung Admin, con của Quiz
    question       TEXT   NOT NULL,
    correct_answer VARCHAR(500) NOT NULL
);
CREATE INDEX idx_quiz_questions_quiz_id ON quiz_questions(quiz_id);

-- Bảng phụ cho @ElementCollection List<String> options — Hibernate tự quản lý insert/delete
CREATE TABLE quiz_question_options (
    question_id BIGINT       NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    option_text VARCHAR(500) NOT NULL
);
CREATE INDEX idx_quiz_question_options_question_id ON quiz_question_options(question_id);

CREATE TABLE quiz_attempts (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quiz_id          BIGINT      NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    score            INTEGER     NOT NULL,
    correct_answers  INTEGER     NOT NULL,
    total_questions  INTEGER     NOT NULL,
    completed_at     TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_quiz_attempts_user_id ON quiz_attempts(user_id);
CREATE INDEX idx_quiz_attempts_quiz_id ON quiz_attempts(quiz_id);

CREATE TABLE user_progress (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id         BIGINT      NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    status           VARCHAR(20) NOT NULL,
    progress_percent INTEGER     NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_progress_user_topic UNIQUE (user_id, topic_id)  -- đúng rule Requirements mục 4
);
CREATE INDEX idx_user_progress_user_id ON user_progress(user_id);