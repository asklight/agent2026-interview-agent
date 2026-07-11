package com.agent2026.interview.projectdeepdive.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("project_profile")
public class ProjectProfileEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String accessTokenHash;
    private String sanitizedDescription;
    private String projectName;
    private String summary;
    private String techStackJson;
    private String responsibilitiesJson;
    private String metricsJson;
    private String architectureJson;
    private String uncertaintiesJson;
    private String analysisStatus;
    private Long version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccessTokenHash() { return accessTokenHash; }
    public void setAccessTokenHash(String accessTokenHash) { this.accessTokenHash = accessTokenHash; }
    public String getSanitizedDescription() { return sanitizedDescription; }
    public void setSanitizedDescription(String sanitizedDescription) { this.sanitizedDescription = sanitizedDescription; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getTechStackJson() { return techStackJson; }
    public void setTechStackJson(String techStackJson) { this.techStackJson = techStackJson; }
    public String getResponsibilitiesJson() { return responsibilitiesJson; }
    public void setResponsibilitiesJson(String responsibilitiesJson) { this.responsibilitiesJson = responsibilitiesJson; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public String getArchitectureJson() { return architectureJson; }
    public void setArchitectureJson(String architectureJson) { this.architectureJson = architectureJson; }
    public String getUncertaintiesJson() { return uncertaintiesJson; }
    public void setUncertaintiesJson(String uncertaintiesJson) { this.uncertaintiesJson = uncertaintiesJson; }
    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
