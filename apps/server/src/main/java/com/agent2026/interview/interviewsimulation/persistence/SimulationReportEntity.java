package com.agent2026.interview.interviewsimulation.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("simulation_report")
public class SimulationReportEntity {
 @TableId(type=IdType.AUTO) private Long id;
 private Long simulationSessionId; private String reportJson; private Integer schemaVersion; private LocalDateTime generatedAt;
}
