package com.agent2026.interview.projectdeepdive.interview.domain;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

public record TurnEvaluationResult(Map<String, Integer> scores, List<String> hitPoints,
                                   List<String> missingPoints, List<String> weaknesses,
                                   List<String> evidence, List<String> riskFlags,
                                   String suggestedDecision, String suggestedFollowUp,
                                   String modelResponseHash, boolean degraded) {
    public TurnEvaluationResult {
        scores = scores == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(scores));
        hitPoints = hitPoints == null ? List.of() : List.copyOf(hitPoints);
        missingPoints = missingPoints == null ? List.of() : List.copyOf(missingPoints);
        weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        riskFlags = riskFlags == null ? List.of() : List.copyOf(riskFlags);
    }
}
