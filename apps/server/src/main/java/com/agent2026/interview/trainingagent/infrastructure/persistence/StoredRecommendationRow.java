package com.agent2026.interview.trainingagent.infrastructure.persistence;

import java.time.LocalDateTime;

public record StoredRecommendationRow(
        long revision,
        String state,
        String trainingType,
        String dimensionCode,
        String title,
        String reason,
        int estimatedMinutes,
        String actionJson,
        String alternativesJson,
        String evidenceIdsJson,
        String policyVersion,
        LocalDateTime generatedAt,
        LocalDateTime expiresAt) {
}
