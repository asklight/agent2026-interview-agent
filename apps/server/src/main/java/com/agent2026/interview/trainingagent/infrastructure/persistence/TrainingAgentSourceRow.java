package com.agent2026.interview.trainingagent.infrastructure.persistence;

import java.time.LocalDateTime;

/** 已完成训练报告的统一读取模型，不把源模块的表结构泄漏到领域层。 */
public record TrainingAgentSourceRow(
        String sourceType,
        Long userId,
        Long sourceSessionId,
        Long sourceReportId,
        int sourceReportVersion,
        String module,
        String reportJson,
        String strengths,
        String weaknesses,
        String recommendations,
        LocalDateTime observedAt) {
}
