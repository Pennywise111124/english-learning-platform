package com.example.englishlearningplatform.dto.topic;

import com.example.englishlearningplatform.entity.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Giống hệt CreateRequest về field, nhưng tách class riêng — vì PUT (update)
// và POST (create) có thể phân kỳ yêu cầu validation trong tương lai
// (VD: sau này create bắt buộc field X nhưng update thì không).
// Giữ 2 class riêng từ đầu tránh phải tách lại khi đã có nhiều nơi dùng chung 1 class.
public class TopicUpdateRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 500)
    private String description;

    @NotNull
    private Level level;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }
}