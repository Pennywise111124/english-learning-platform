-- V11: M14 Content Status. Dòng cũ thành PUBLISHED để nội dung đang chạy không biến mất.

-- 1. Thêm cột status với giá trị mặc định tạm thời là 'PUBLISHED' cho các dòng hiện tại
ALTER TABLE topics            ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';
ALTER TABLE quizzes           ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';
ALTER TABLE dictation_lessons ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';

-- 2. Thêm ràng buộc CHECK ràng buộc giá trị hợp lệ ('DRAFT', 'PUBLISHED', 'ARCHIVED')
ALTER TABLE topics            ADD CONSTRAINT ck_topics_status            CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));
ALTER TABLE quizzes           ADD CONSTRAINT ck_quizzes_status           CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));
ALTER TABLE dictation_lessons ADD CONSTRAINT ck_dictation_lessons_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));

-- 3. Bỏ DEFAULT để ép Entity/Application Layer luôn phải truyền status rõ ràng khi INSERT
ALTER TABLE topics            ALTER COLUMN status DROP DEFAULT;
ALTER TABLE quizzes           ALTER COLUMN status DROP DEFAULT;
ALTER TABLE dictation_lessons ALTER COLUMN status DROP DEFAULT;