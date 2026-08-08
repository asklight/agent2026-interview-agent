package com.agent2026.interview.algorithmpractice.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("algorithm_report")
public class AlgorithmReportEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String reportJson;
    private Integer schemaVersion;
    private LocalDateTime generatedAt;
}
