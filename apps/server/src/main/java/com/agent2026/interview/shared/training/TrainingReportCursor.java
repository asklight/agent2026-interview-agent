package com.agent2026.interview.shared.training;

import java.time.LocalDateTime;

public record TrainingReportCursor(LocalDateTime completedAt, Long sourceSessionId) {
    public TrainingReportCursor {
        if (completedAt == null || sourceSessionId == null) {
            throw new IllegalArgumentException("training report cursor fields must not be null");
        }
    }
}
