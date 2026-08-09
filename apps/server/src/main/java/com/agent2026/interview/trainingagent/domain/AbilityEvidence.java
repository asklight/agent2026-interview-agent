package com.agent2026.interview.trainingagent.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record AbilityEvidence(
        Long id,
        Long userId,
        String sourceType,
        Long sourceSessionId,
        Long sourceReportId,
        int sourceReportVersion,
        String evidenceKey,
        AbilityDimension dimension,
        EvidencePolarity polarity,
        int severity,
        double confidence,
        String text,
        Long sourceTurnId,
        Long sourceEvaluationId,
        Map<String, String> metadata,
        LocalDateTime observedAt) {
}
