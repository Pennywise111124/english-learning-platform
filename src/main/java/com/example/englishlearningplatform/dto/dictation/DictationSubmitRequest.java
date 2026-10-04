package com.example.englishlearningplatform.dto.dictation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DictationSubmitRequest(
        @NotBlank(message = "userInput is required") @Size(max = 5000) String userInput) {
}
