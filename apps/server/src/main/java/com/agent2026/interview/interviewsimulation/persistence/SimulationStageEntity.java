package com.agent2026.interview.interviewsimulation.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("simulation_stage")
public class SimulationStageEntity {
 @TableId(type=IdType.AUTO) private Long id;
 private Long simulationSessionId; private String stageType; private Integer sequenceNo;
 private Long businessSessionId; private String status; private LocalDateTime startedAt; private LocalDateTime finishedAt;
}
