package com.agent2026.interview.traininghistory.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("training_history")
public class TrainingHistoryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String trainingType;
    private Long sourceSessionId;
    private String status;
    private String title;
    private String summary;
    private Boolean hidden;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
