package com.agent2026.interview.algorithmpractice.api;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AlgorithmReportResponse(int schemaVersion, Long sessionId, String completionStatus,
                                      Double overallScore, double coverage,
                                      List<DimensionResult> dimensions,
                                      List<EvidenceConclusion> strengths,
                                      List<EvidenceConclusion> gaps,
                                      List<String> recommendations,
                                      List<RoundReview> rounds, LocalDateTime generatedAt) {
    public AlgorithmReportResponse {
        dimensions = copy(dimensions);
        strengths = copy(strengths);
        gaps = copy(gaps);
        recommendations = copy(recommendations);
        rounds = copy(rounds);
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record DimensionResult(String dimension, String status, Integer score) {}

    public record EvidenceConclusion(String text, Long candidateTurnId, Long evaluationId,
                                     String candidateEvidence) {}

    public record RoundReview(int sequence, String stage, String candidateAnswer,
                              Map<String, Integer> scores, List<String> strengths,
                              List<String> gaps, List<String> evidence,
                              Long candidateTurnId, Long evaluationId) {
        public RoundReview {
            scores = scores == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(scores));
            strengths = copy(strengths);
            gaps = copy(gaps);
            evidence = copy(evidence);
        }
    }
}
