package com.agent2026.interview.algorithmpractice.domain;

import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record AlgorithmEvaluation(
        Map<String, Integer> scores,
        List<String> strengths,
        List<String> gaps,
        List<String> evidence,
        String suggestedFollowUp,
        String modelResponseHash,
        boolean degraded
) {
    public AlgorithmEvaluation {
        scores = scores == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(scores));
        strengths = strengths == null ? List.of() : List.copyOf(strengths);
        gaps = gaps == null ? List.of() : List.copyOf(gaps);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
