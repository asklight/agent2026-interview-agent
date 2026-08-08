package com.agent2026.interview.algorithmpractice.api;

import java.util.List;

public record AlgorithmProblemResponse(Long id, String code, String title, String statement,
                                       String difficulty, List<String> tags, List<String> constraints) {
    public AlgorithmProblemResponse {
        tags = tags == null ? List.of() : List.copyOf(tags);
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
    }
}
