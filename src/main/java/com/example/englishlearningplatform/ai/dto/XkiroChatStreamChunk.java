package com.example.englishlearningplatform.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record XkiroChatStreamChunk(List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Delta delta) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Delta(String content) {
    }
}