package com.agent2026.interview.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class InterviewReportVO {
    private Long sessionId;
    private BigDecimal totalScore;
    private String scoreLevel;
    private Integer answeredCount;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;
    private LocalDateTime generatedAt;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getScoreLevel() { return scoreLevel; }
    public void setScoreLevel(String scoreLevel) { this.scoreLevel = scoreLevel; }
    public Integer getAnsweredCount() { return answeredCount; }
    public void setAnsweredCount(Integer answeredCount) { this.answeredCount = answeredCount; }
    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }
    public List<String> getWeaknesses() { return weaknesses; }
    public void setWeaknesses(List<String> weaknesses) { this.weaknesses = weaknesses; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
