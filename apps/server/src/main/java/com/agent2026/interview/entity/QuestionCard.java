package com.agent2026.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("question_card")
public class QuestionCard {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String cardCode;
    private String module;
    private String difficulty;
    private String mainQuestion;
    private String keyPoints;
    private String commonMistakes;
    private String followups;
    private String scenarioFollowups;
    private String scoringRubric;
    private String tags;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardCode() {
        return cardCode;
    }

    public void setCardCode(String cardCode) {
        this.cardCode = cardCode;
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

    public String getMainQuestion() {
        return mainQuestion;
    }

    public void setMainQuestion(String mainQuestion) {
        this.mainQuestion = mainQuestion;
    }

    public String getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(String keyPoints) {
        this.keyPoints = keyPoints;
    }

    public String getCommonMistakes() {
        return commonMistakes;
    }

    public void setCommonMistakes(String commonMistakes) {
        this.commonMistakes = commonMistakes;
    }

    public String getFollowups() {
        return followups;
    }

    public void setFollowups(String followups) {
        this.followups = followups;
    }

    public String getScenarioFollowups() {
        return scenarioFollowups;
    }

    public void setScenarioFollowups(String scenarioFollowups) {
        this.scenarioFollowups = scenarioFollowups;
    }

    public String getScoringRubric() {
        return scoringRubric;
    }

    public void setScoringRubric(String scoringRubric) {
        this.scoringRubric = scoringRubric;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
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
