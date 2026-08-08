package com.agent2026.interview.algorithmpractice.application;

import com.agent2026.interview.algorithmpractice.api.AlgorithmReportResponse;
import com.agent2026.interview.algorithmpractice.domain.AlgorithmEvaluation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AlgorithmReportAggregator {
    public static final List<String> DIMENSIONS = List.of(
            "correctness", "optimization", "complexity", "edgeCases", "communication");

    public AlgorithmReportResponse aggregate(Long sessionId, List<EvaluationFact> facts, LocalDateTime generatedAt) {
        Map<String, List<Integer>> values = new LinkedHashMap<>();
        DIMENSIONS.forEach(dimension -> values.put(dimension, new ArrayList<>()));
        List<AlgorithmReportResponse.EvidenceConclusion> strengths = new ArrayList<>();
        List<AlgorithmReportResponse.EvidenceConclusion> gaps = new ArrayList<>();
        List<AlgorithmReportResponse.RoundReview> rounds = new ArrayList<>();

        int sequence = 1;
        for (EvaluationFact fact : facts) {
            AlgorithmEvaluation evaluation = fact.evaluation();
            evaluation.scores().forEach((dimension, score) -> {
                if (values.containsKey(dimension) && score != null && score >= 0 && score <= 100) {
                    values.get(dimension).add(score);
                }
            });
            String evidence = evaluation.evidence().isEmpty()
                    ? excerpt(fact.candidateAnswer()) : evaluation.evidence().get(0);
            evaluation.strengths().forEach(text -> strengths.add(conclusion(text, fact, evidence)));
            evaluation.gaps().forEach(text -> gaps.add(conclusion(text, fact, evidence)));
            rounds.add(new AlgorithmReportResponse.RoundReview(sequence++, fact.stage(), fact.candidateAnswer(),
                    evaluation.scores(), evaluation.strengths(), evaluation.gaps(), evaluation.evidence(),
                    fact.candidateTurnId(), fact.evaluationId()));
        }

        List<AlgorithmReportResponse.DimensionResult> dimensions = new ArrayList<>();
        List<Integer> assessed = new ArrayList<>();
        for (String dimension : DIMENSIONS) {
            List<Integer> scores = values.get(dimension);
            Integer score = scores.isEmpty() ? null
                    : (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
            if (score != null) assessed.add(score);
            dimensions.add(new AlgorithmReportResponse.DimensionResult(dimension,
                    score == null ? "NOT_ASSESSED" : "ASSESSED", score));
        }
        Double overall = assessed.isEmpty() ? null : roundOne(
                assessed.stream().mapToInt(Integer::intValue).average().orElse(0));
        double coverage = roundOne(assessed.size() * 100.0 / DIMENSIONS.size());
        List<String> recommendations = gaps.stream().map(AlgorithmReportResponse.EvidenceConclusion::text)
                .distinct().limit(3).toList();
        if (recommendations.isEmpty() && !facts.isEmpty()) {
            recommendations = List.of("继续用清晰的约束、方案、复杂度和边界结构完整口述解题过程。");
        }
        String completion = facts.size() >= 6 ? "COMPLETE" : "PARTIAL";
        return new AlgorithmReportResponse(1, sessionId, completion, overall, coverage, dimensions,
                strengths, gaps, recommendations, rounds, generatedAt);
    }

    private AlgorithmReportResponse.EvidenceConclusion conclusion(String text, EvaluationFact fact, String evidence) {
        return new AlgorithmReportResponse.EvidenceConclusion(
                text, fact.candidateTurnId(), fact.evaluationId(), evidence);
    }

    private String excerpt(String text) {
        if (text == null) return "";
        String normalized = text.strip();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record EvaluationFact(Long evaluationId, Long candidateTurnId, String stage,
                                 String candidateAnswer, AlgorithmEvaluation evaluation) {}
}
