package com.example.englishlearningplatform.entity;

public enum ProgressStatus {
    NOT_STARTED, // Chỉ dùng ở tầng logic/response khi KHÔNG có record nào trong DB —
                 // không bao giờ thực sự được lưu (record chỉ tạo lazy khi đã có hoạt động,
                 // nghĩa là ngay khi có record thì tối thiểu đã là IN_PROGRESS)
    IN_PROGRESS,
    COMPLETED
}