package com.example.englishlearningplatform.exception;

/**
 * Throw khi Admin cố xoá 1 resource đang có dữ liệu phụ thuộc (VD: Quiz đã có
 * QuizAttempt, Topic đã có UserProgress) — theo rule Content deletion mục 2.3.
 * Map ra 409 Conflict — khác ResourceNotFoundException (404, resource không
 * tồn tại) và IllegalArgumentException (400, input sai) — đây là "resource có
 * tồn tại thật, nhưng thao tác bị chặn vì trạng thái hiện tại của hệ thống".
 */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }
}