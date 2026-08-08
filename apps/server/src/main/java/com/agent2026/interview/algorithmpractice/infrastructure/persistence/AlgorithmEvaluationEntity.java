package com.agent2026.interview.algorithmpractice.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("algorithm_turn_evaluation")
public class AlgorithmEvaluationEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long candidateTurnId;
    private String stage;
    private String evaluationJson;
    private String modelResponseHash;
    private Boolean degraded;
    private LocalDateTime createTime;
}
