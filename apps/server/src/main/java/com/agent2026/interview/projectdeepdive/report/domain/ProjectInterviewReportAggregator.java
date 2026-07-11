package com.agent2026.interview.projectdeepdive.report.domain;

import com.agent2026.interview.projectdeepdive.report.api.ProjectInterviewReportResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ProjectInterviewReportAggregator {
    private static final List<String> DIMENSIONS = List.of(
            "authenticity", "ownership", "technicalDepth", "tradeoffReasoning", "engineeringAwareness", "communication");

    public ProjectInterviewReportResponse aggregate(Long sessionId, List<ReportEvaluationFact> facts, LocalDateTime generatedAt) {
        Map<String, List<Integer>> values = new LinkedHashMap<>();
        DIMENSIONS.forEach(d -> values.put(d, new ArrayList<>()));
        List<ProjectInterviewReportResponse.Conclusion> strengths = new ArrayList<>();
        List<ProjectInterviewReportResponse.Conclusion> risks = new ArrayList<>();
        List<ProjectInterviewReportResponse.Conclusion> weaknesses = new ArrayList<>();
        List<ProjectInterviewReportResponse.Conclusion> recommendations = new ArrayList<>();
        List<ProjectInterviewReportResponse.RoundReview> rounds = new ArrayList<>();
        Map<Long, ClaimBucket> claims = new LinkedHashMap<>();

        int sequence = 1;
        for (ReportEvaluationFact fact : facts) {
            String primary = dimensionForProbe(fact.probeDimension());
            Integer primaryScore = scoreFor(fact.scores(), primary);
            if (primaryScore != null && primary != null) values.get(primary).add(primaryScore);
            Integer communication = scoreFor(fact.scores(), "communication");
            if (communication != null && !"communication".equals(primary)) values.get("communication").add(communication);
            ClaimBucket bucket = claims.computeIfAbsent(fact.claimId(), ignored -> new ClaimBucket());
            fact.hitPoints().forEach(text -> { var item = conclusion(text, fact); strengths.add(item); bucket.strengths.add(item); });
            fact.riskFlags().forEach(text -> { var item = conclusion(text, fact); risks.add(item); bucket.risks.add(item); });
            fact.weaknesses().forEach(text -> { var item = conclusion(text, fact); weaknesses.add(item); bucket.weaknesses.add(item); });
            fact.missingPoints().forEach(text -> { var item = conclusion("建议补充：" + text, fact); recommendations.add(item); bucket.recommendations.add(item); });
            rounds.add(new ProjectInterviewReportResponse.RoundReview(sequence++, fact.probeDimension(), fact.candidateAnswer(),
                    fact.hitPoints(), fact.missingPoints(), fact.riskFlags(), fact.claimId(), fact.candidateTurnId(), fact.evaluationId()));
        }

        List<ProjectInterviewReportResponse.DimensionResult> dimensions = new ArrayList<>();
        List<Integer> assessed = new ArrayList<>();
        for (String dimension : DIMENSIONS) {
            List<Integer> scores = values.get(dimension);
            Integer score = scores.isEmpty() ? null : (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
            if (score != null) assessed.add(score);
            dimensions.add(new ProjectInterviewReportResponse.DimensionResult(dimension,
                    score == null ? "NOT_ASSESSED" : "ASSESSED", score));
        }
        Double total = assessed.isEmpty() ? null : roundOne(assessed.stream().mapToInt(Integer::intValue).average().orElse(0));
        double coverage = roundOne(assessed.size() * 100.0 / DIMENSIONS.size());
        if (recommendations.isEmpty() && !facts.isEmpty()) {
            ReportEvaluationFact last = facts.get(facts.size() - 1);
            var item = conclusion("继续使用“背景—个人动作—技术依据—结果—反思”的结构回答。", last);
            recommendations.add(item); claims.computeIfAbsent(last.claimId(), ignored -> new ClaimBucket()).recommendations.add(item);
        }
        List<ProjectInterviewReportResponse.ClaimReview> claimReviews = claims.entrySet().stream()
                .map(entry -> new ProjectInterviewReportResponse.ClaimReview(entry.getKey(), entry.getValue().strengths,
                        entry.getValue().risks, entry.getValue().weaknesses, entry.getValue().recommendations)).toList();
        return new ProjectInterviewReportResponse(1, sessionId, "PROJECT_DEEP_DIVE", "COMPLETED", total, coverage,
                dimensions, strengths, risks, weaknesses, recommendations, claimReviews, rounds, generatedAt);
    }

    private ProjectInterviewReportResponse.Conclusion conclusion(String text, ReportEvaluationFact fact) {
        return new ProjectInterviewReportResponse.Conclusion(text, fact.claimId(), fact.candidateTurnId(), fact.evaluationId());
    }

    private String dimensionForProbe(String probe) {
        if (probe == null) return null;
        return switch (probe.toUpperCase(Locale.ROOT)) {
            case "AUTHENTICITY" -> "authenticity";
            case "OWNERSHIP" -> "ownership";
            case "PRINCIPLE" -> "technicalDepth";
            case "TRADEOFF" -> "tradeoffReasoning";
            case "INCIDENT", "SCALE", "METRIC" -> "engineeringAwareness";
            default -> null;
        };
    }

    private Integer scoreFor(Map<String, Integer> scores, String dimension) {
        if (dimension == null || scores == null || scores.isEmpty()) return null;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (normalize(entry.getKey()).equals(normalize(dimension))) return valid(entry.getValue());
        }
        if (!"communication".equals(dimension) && scores.size() == 1) return valid(scores.values().iterator().next());
        return null;
    }
    private Integer valid(Integer value) { return value != null && value >= 0 && value <= 100 ? value : null; }
    private String normalize(String value) { return value == null ? "" : value.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT); }
    private double roundOne(double value) { return Math.round(value * 10.0) / 10.0; }

    private static final class ClaimBucket {
        private final List<ProjectInterviewReportResponse.Conclusion> strengths = new ArrayList<>();
        private final List<ProjectInterviewReportResponse.Conclusion> risks = new ArrayList<>();
        private final List<ProjectInterviewReportResponse.Conclusion> weaknesses = new ArrayList<>();
        private final List<ProjectInterviewReportResponse.Conclusion> recommendations = new ArrayList<>();
    }
}
