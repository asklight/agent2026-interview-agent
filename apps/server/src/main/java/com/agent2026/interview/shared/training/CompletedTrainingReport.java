package com.agent2026.interview.shared.training;

import java.time.LocalDateTime;

public record CompletedTrainingReport(
        String sourceType,
        Long userId,
        Long sourceSessionId,
        Long sourceReportId,
        int sourceReportVersion,
        String module,
        String difficulty,
        String tags,
        Long projectProfileId,
        String reportJson,
        String strengths,
        String weaknesses,
        String recommendations,
        LocalDateTime observedAt) {
}
