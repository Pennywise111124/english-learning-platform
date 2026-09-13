package com.example.englishlearningplatform.ai.dto;

import java.util.List;
import java.util.Map;

public record XkiroChatRequest(
                String model,
                List<Map<String, String>> messages,
                Boolean stream) {
}