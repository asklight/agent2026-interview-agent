package com.agent2026.interview.param;

import jakarta.validation.constraints.NotBlank;

public class SubmitAnswerParam {

    @NotBlank(message = "answerText cannot be blank")
    private String answerText;

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
}
