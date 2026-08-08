package com.agent2026.interview.interviewsimulation.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("simulation_session")
public class SimulationSessionEntity {
 @TableId(type=IdType.AUTO) private Long id;
 private Long userId; private String clientRequestId; private Long projectProfileId; private Long algorithmProblemId;
 private String status; private String currentStage; private Long version;
 private LocalDateTime startedAt; private LocalDateTime finishedAt; private LocalDateTime createTime; private LocalDateTime updateTime;
}
