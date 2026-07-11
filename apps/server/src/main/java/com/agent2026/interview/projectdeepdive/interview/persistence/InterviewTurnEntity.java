package com.agent2026.interview.projectdeepdive.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("interview_turn")
public class InterviewTurnEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long sessionId; private Integer sequenceNo; private String role; private String turnType;
    private String content; private String inputModality; private Long parentTurnId; private Long claimId;
    private String probeId; private String probeDimension; private String processingStatus;
    private LocalDateTime processingStartedAt; private String clientTurnId;
    private LocalDateTime startedAt; private LocalDateTime endedAt; private LocalDateTime createTime;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getSessionId(){return sessionId;} public void setSessionId(Long v){sessionId=v;}
    public Integer getSequenceNo(){return sequenceNo;} public void setSequenceNo(Integer v){sequenceNo=v;} public String getRole(){return role;} public void setRole(String v){role=v;}
    public String getTurnType(){return turnType;} public void setTurnType(String v){turnType=v;} public String getContent(){return content;} public void setContent(String v){content=v;}
    public String getInputModality(){return inputModality;} public void setInputModality(String v){inputModality=v;} public Long getParentTurnId(){return parentTurnId;} public void setParentTurnId(Long v){parentTurnId=v;}
    public Long getClaimId(){return claimId;} public void setClaimId(Long v){claimId=v;} public String getProbeId(){return probeId;} public void setProbeId(String v){probeId=v;}
    public String getProbeDimension(){return probeDimension;} public void setProbeDimension(String v){probeDimension=v;} public String getProcessingStatus(){return processingStatus;} public void setProcessingStatus(String v){processingStatus=v;}
    public LocalDateTime getProcessingStartedAt(){return processingStartedAt;} public void setProcessingStartedAt(LocalDateTime v){processingStartedAt=v;} public String getClientTurnId(){return clientTurnId;} public void setClientTurnId(String v){clientTurnId=v;}
    public LocalDateTime getStartedAt(){return startedAt;} public void setStartedAt(LocalDateTime v){startedAt=v;} public LocalDateTime getEndedAt(){return endedAt;} public void setEndedAt(LocalDateTime v){endedAt=v;}
    public LocalDateTime getCreateTime(){return createTime;} public void setCreateTime(LocalDateTime v){createTime=v;}
}
