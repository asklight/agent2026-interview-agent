package com.agent2026.interview.algorithmpractice.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("algorithm_problem")
public class AlgorithmProblemEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String problemCode;
    private String title;
    private String statement;
    private String difficulty;
    private String tags;
    private String constraintsJson;
    private String evaluationRubricJson;
    private String followUpTemplatesJson;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
