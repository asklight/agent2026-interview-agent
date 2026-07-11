package com.agent2026.interview.projectdeepdive.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("interview_plan")
public class InterviewPlanEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long sessionId;
    private String projectProfileSnapshotJson;
    private String plannedProbesJson;
    private Integer templateVersion;
    private String status;
    private LocalDateTime createTime;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getSessionId(){return sessionId;} public void setSessionId(Long v){sessionId=v;}
    public String getProjectProfileSnapshotJson(){return projectProfileSnapshotJson;} public void setProjectProfileSnapshotJson(String v){projectProfileSnapshotJson=v;}
    public String getPlannedProbesJson(){return plannedProbesJson;} public void setPlannedProbesJson(String v){plannedProbesJson=v;}
    public Integer getTemplateVersion(){return templateVersion;} public void setTemplateVersion(Integer v){templateVersion=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public LocalDateTime getCreateTime(){return createTime;} public void setCreateTime(LocalDateTime v){createTime=v;}
}
