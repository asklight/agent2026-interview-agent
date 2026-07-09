package com.agent2026.interview.vo;

public class InterviewSessionVO {

    private Long sessionId;
    private String mode;
    private String module;
    private String difficulty;
    private Integer questionCount;
    private Integer completedQuestionCount;
    private String status;
    private CurrentQuestionVO currentQuestion;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

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

    public Integer getCompletedQuestionCount() {
        return completedQuestionCount;
    }

    public void setCompletedQuestionCount(Integer completedQuestionCount) {
        this.completedQuestionCount = completedQuestionCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public CurrentQuestionVO getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(CurrentQuestionVO currentQuestion) {
        this.currentQuestion = currentQuestion;
    }
}
