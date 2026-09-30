-- M10: cột thời gian tạo cho sort "mới nhất" (FR-9.4).
-- Dòng cũ nhận cùng giá trị NOW(); thứ tự giữa chúng do tie-break theo id đảm nhận.
ALTER TABLE topics
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();