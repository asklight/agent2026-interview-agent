package com.agent2026.interview.shared.training;

import java.time.LocalDateTime;

public record CompletedTrainingReportRef(
        String sourceType,
        Long userId,
        Long sourceSessionId,
        int sourceReportVersion,
        LocalDateTime completedAt) {
}
