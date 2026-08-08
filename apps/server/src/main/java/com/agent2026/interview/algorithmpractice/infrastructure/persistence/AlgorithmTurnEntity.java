package com.agent2026.interview.algorithmpractice.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("algorithm_turn")
public class AlgorithmTurnEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer sequenceNo;
    private String role;
    private String stage;
    private String content;
    private String inputModality;
    private Long parentTurnId;
    private String clientTurnId;
    private String processingStatus;
    private LocalDateTime processingStartedAt;
    private LocalDateTime createTime;
}
