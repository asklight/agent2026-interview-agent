package com.agent2026.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("interview_session")
public class InterviewSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String mode;
    private String module;
    private String difficulty;
    private Integer questionCount;
    private Integer completedQuestionCount;
    private Long currentQuestionId;
    private String currentQuestionType;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String currentFollowUpQuestion;
    private String askedQuestionIds;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getCurrentQuestionId() {
        return currentQuestionId;
    }

    public void setCurrentQuestionId(Long currentQuestionId) {
        this.currentQuestionId = currentQuestionId;
    }

    public String getCurrentQuestionType() {
        return currentQuestionType;
    }

    public void setCurrentQuestionType(String currentQuestionType) {
        this.currentQuestionType = currentQuestionType;
    }

    public String getCurrentFollowUpQuestion() {
        return currentFollowUpQuestion;
    }

    public void setCurrentFollowUpQuestion(String currentFollowUpQuestion) {
        this.currentFollowUpQuestion = currentFollowUpQuestion;
    }

    public String getAskedQuestionIds() {
        return askedQuestionIds;
    }

    public void setAskedQuestionIds(String askedQuestionIds) {
        this.askedQuestionIds = askedQuestionIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
