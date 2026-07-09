package com.agent2026.interview.param;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CreateInterviewSessionParam {

    @NotBlank(message = "module cannot be blank")
    private String module;

    private String difficulty;

    @Min(value = 1, message = "questionCount must be at least 1")
    @Max(value = 20, message = "questionCount must be at most 20")
    private Integer questionCount = 5;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
}
