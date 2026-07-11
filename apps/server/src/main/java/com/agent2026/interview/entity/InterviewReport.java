package com.agent2026.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("interview_report")
public class InterviewReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String mode;
    private String generationStatus;
    private BigDecimal totalScore;
    private String scoreLevel;
    private Integer answeredCount;
    private String strengths;
    private String weaknesses;
    private String recommendations;
    private String reportJson;
    private Integer schemaVersion;
    private LocalDateTime generatedAt;
    private String errorCode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getGenerationStatus() { return generationStatus; }
    public void setGenerationStatus(String generationStatus) { this.generationStatus = generationStatus; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getScoreLevel() { return scoreLevel; }
    public void setScoreLevel(String scoreLevel) { this.scoreLevel = scoreLevel; }
    public Integer getAnsweredCount() { return answeredCount; }
    public void setAnsweredCount(Integer answeredCount) { this.answeredCount = answeredCount; }
    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }
    public String getWeaknesses() { return weaknesses; }
    public void setWeaknesses(String weaknesses) { this.weaknesses = weaknesses; }
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public String getReportJson() { return reportJson; }
    public void setReportJson(String reportJson) { this.reportJson = reportJson; }
    public Integer getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(Integer schemaVersion) { this.schemaVersion = schemaVersion; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
