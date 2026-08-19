package com.agent2026.interview.shared.training;

import java.time.LocalDateTime;

public record TrainingCompletedEvent(Long userId, String sourceType, Long sourceSessionId,
                                     int sourceReportVersion, LocalDateTime completedAt) {
    public TrainingCompletedEvent {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId must be positive");
        if (sourceType == null || sourceType.isBlank()) throw new IllegalArgumentException("sourceType is required");
        if (sourceSessionId == null || sourceSessionId <= 0) {
            throw new IllegalArgumentException("sourceSessionId must be positive");
        }
        if (sourceReportVersion <= 0) throw new IllegalArgumentException("sourceReportVersion must be positive");
        if (completedAt == null) completedAt = LocalDateTime.now();
    }
}
