package com.agent2026.interview.projectdeepdive.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("turn_evaluation")
public class TurnEvaluationEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long sessionId; private Long candidateTurnId; private String probeId;
    private String scoreJson; private String hitPointsJson; private String missingPointsJson;
    private String weaknessesJson; private String evidenceJson; private String riskFlagsJson;
    private String decision; private String suggestedFollowUp; private String retrievalTraceJson;
    private String modelResponseHash; private Integer modelSchemaVersion; private Boolean degraded; private LocalDateTime createTime;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getSessionId(){return sessionId;} public void setSessionId(Long v){sessionId=v;}
    public Long getCandidateTurnId(){return candidateTurnId;} public void setCandidateTurnId(Long v){candidateTurnId=v;} public String getProbeId(){return probeId;} public void setProbeId(String v){probeId=v;}
    public String getScoreJson(){return scoreJson;} public void setScoreJson(String v){scoreJson=v;} public String getHitPointsJson(){return hitPointsJson;} public void setHitPointsJson(String v){hitPointsJson=v;}
    public String getMissingPointsJson(){return missingPointsJson;} public void setMissingPointsJson(String v){missingPointsJson=v;} public String getWeaknessesJson(){return weaknessesJson;} public void setWeaknessesJson(String v){weaknessesJson=v;}
    public String getEvidenceJson(){return evidenceJson;} public void setEvidenceJson(String v){evidenceJson=v;} public String getRiskFlagsJson(){return riskFlagsJson;} public void setRiskFlagsJson(String v){riskFlagsJson=v;}
    public String getDecision(){return decision;} public void setDecision(String v){decision=v;} public String getSuggestedFollowUp(){return suggestedFollowUp;} public void setSuggestedFollowUp(String v){suggestedFollowUp=v;}
    public String getRetrievalTraceJson(){return retrievalTraceJson;} public void setRetrievalTraceJson(String v){retrievalTraceJson=v;} public String getModelResponseHash(){return modelResponseHash;} public void setModelResponseHash(String v){modelResponseHash=v;}
    public Integer getModelSchemaVersion(){return modelSchemaVersion;} public void setModelSchemaVersion(Integer v){modelSchemaVersion=v;} public Boolean getDegraded(){return degraded;} public void setDegraded(Boolean v){degraded=v;}
    public LocalDateTime getCreateTime(){return createTime;} public void setCreateTime(LocalDateTime v){createTime=v;}
}
