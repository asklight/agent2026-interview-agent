package com.agent2026.interview.projectdeepdive.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("project_claim")
public class ProjectClaimEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectProfileId;
    private String claimType;
    private String statement;
    private String sourceFragment;
    private String relatedTechnologiesJson;
    private String expectedEvidenceJson;
    private String riskLevel;
    private Boolean confirmed;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectProfileId() { return projectProfileId; }
    public void setProjectProfileId(Long projectProfileId) { this.projectProfileId = projectProfileId; }
    public String getClaimType() { return claimType; }
    public void setClaimType(String claimType) { this.claimType = claimType; }
    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }
    public String getSourceFragment() { return sourceFragment; }
    public void setSourceFragment(String sourceFragment) { this.sourceFragment = sourceFragment; }
    public String getRelatedTechnologiesJson() { return relatedTechnologiesJson; }
    public void setRelatedTechnologiesJson(String relatedTechnologiesJson) { this.relatedTechnologiesJson = relatedTechnologiesJson; }
    public String getExpectedEvidenceJson() { return expectedEvidenceJson; }
    public void setExpectedEvidenceJson(String expectedEvidenceJson) { this.expectedEvidenceJson = expectedEvidenceJson; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Boolean getConfirmed() { return confirmed; }
    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
