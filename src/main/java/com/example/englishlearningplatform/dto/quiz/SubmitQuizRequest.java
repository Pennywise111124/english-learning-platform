package com.example.englishlearningplatform.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class SubmitQuizRequest {

    @NotEmpty
    @Valid // BẮT BUỘC — nếu thiếu, Bean Validation KHÔNG validate xuống từng
           // phần tử bên trong List<SubmitAnswerItem>, dù mỗi item có @NotNull/@NotBlank
    private List<SubmitAnswerItem> answers;

    public List<SubmitAnswerItem> getAnswers() {
        return answers;
    }

    public void setAnswers(List<SubmitAnswerItem> answers) {
        this.answers = answers;
    }
}