package com.example.englishlearningplatform.dto.common;

import com.example.englishlearningplatform.entity.ContentStatus;

import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(@NotNull ContentStatus status) {
}