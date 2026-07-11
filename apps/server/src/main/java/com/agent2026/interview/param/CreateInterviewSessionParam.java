package com.agent2026.interview.param;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class CreateInterviewSessionParam {

    private String mode = "JAVA_CORE";
    private String module;

    private String difficulty;

    @Min(value = 1, message = "questionCount must be at least 1")
    @Max(value = 20, message = "questionCount must be at most 20")
    private Integer questionCount = 5;

    private Long projectProfileId;

    @Min(value = 5, message = "durationMinutes must be at least 5")
    @Max(value = 120, message = "durationMinutes must be at most 120")
    private Integer durationMinutes = 20;

    @Min(value = 0, message = "maxFollowUpsPerClaim must be at least 0")
    @Max(value = 5, message = "maxFollowUpsPerClaim must be at most 5")
    private Integer maxFollowUpsPerClaim = 3;

    private String inputModality = "TEXT";

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

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

    public Long getProjectProfileId() { return projectProfileId; }
    public void setProjectProfileId(Long projectProfileId) { this.projectProfileId = projectProfileId; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Integer getMaxFollowUpsPerClaim() { return maxFollowUpsPerClaim; }
    public void setMaxFollowUpsPerClaim(Integer maxFollowUpsPerClaim) { this.maxFollowUpsPerClaim = maxFollowUpsPerClaim; }
    public String getInputModality() { return inputModality; }
    public void setInputModality(String inputModality) { this.inputModality = inputModality; }
}
