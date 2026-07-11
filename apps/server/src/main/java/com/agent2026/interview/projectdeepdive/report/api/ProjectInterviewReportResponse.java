package com.agent2026.interview.projectdeepdive.report.api;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectInterviewReportResponse(
        int schemaVersion, Long sessionId, String mode, String generationStatus,
        Double totalScore, double coverageRate, List<DimensionResult> dimensions,
        List<Conclusion> strengths, List<Conclusion> risks, List<Conclusion> weaknesses,
        List<Conclusion> recommendations, List<ClaimReview> claimReviews,
        List<RoundReview> rounds, LocalDateTime generatedAt) {
    public ProjectInterviewReportResponse {
        dimensions = safe(dimensions); strengths = safe(strengths); risks = safe(risks);
        weaknesses = safe(weaknesses); recommendations = safe(recommendations);
        claimReviews = safe(claimReviews); rounds = safe(rounds);
    }
    private static <T> List<T> safe(List<T> values) { return values == null ? List.of() : List.copyOf(values); }

    public record DimensionResult(String dimension, String status, Integer score) {}
    public record Conclusion(String text, Long claimId, Long candidateTurnId, Long evaluationId) {}
    public record ClaimReview(Long claimId, List<Conclusion> strengths, List<Conclusion> risks,
                              List<Conclusion> weaknesses, List<Conclusion> recommendations) {
        public ClaimReview {
            strengths = safe(strengths); risks = safe(risks); weaknesses = safe(weaknesses); recommendations = safe(recommendations);
        }
    }
    public record RoundReview(int sequenceNo, String dimension, String candidateAnswer,
                              List<String> hitPoints, List<String> missingPoints, List<String> riskFlags,
                              Long claimId, Long candidateTurnId, Long evaluationId) {
        public RoundReview {
            hitPoints = safe(hitPoints); missingPoints = safe(missingPoints); riskFlags = safe(riskFlags);
        }
    }
}
