package com.example.englishlearningplatform.dto.dictation;

import com.example.englishlearningplatform.entity.Level;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DictationLessonRequest(
                @NotBlank(message = "title is required") @Size(max = 255) String title,
                @NotBlank(message = "transcript is required") @Size(max = 5000) String transcript,
                @NotNull(message = "level is required") Level level) {
}