package com.agent2026.interview.vo;

public class SubmitAnswerVO {

    private Long answerId;
    private Long sessionId;
    private Long questionId;
    private String evaluationText;
    private String nextAction;
    private String followUpQuestion;
    private CurrentQuestionVO currentQuestion;
    private Integer completedQuestionCount;
    private Integer questionCount;

    public Long getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Long answerId) {
        this.answerId = answerId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getEvaluationText() {
        return evaluationText;
    }

    public void setEvaluationText(String evaluationText) {
        this.evaluationText = evaluationText;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public String getFollowUpQuestion() {
        return followUpQuestion;
    }

    public void setFollowUpQuestion(String followUpQuestion) {
        this.followUpQuestion = followUpQuestion;
    }

    public CurrentQuestionVO getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(CurrentQuestionVO currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public Integer getCompletedQuestionCount() {
        return completedQuestionCount;
    }

    public void setCompletedQuestionCount(Integer completedQuestionCount) {
        this.completedQuestionCount = completedQuestionCount;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
}
