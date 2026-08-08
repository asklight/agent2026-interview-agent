package com.agent2026.interview.algorithmpractice.api;

import java.util.List;

public record AlgorithmSessionResponse(Long sessionId, String status, String currentStage, long version,
                                       String turnState, AlgorithmProblemResponse problem,
                                       List<AlgorithmTurnResponse> turns) {
    public AlgorithmSessionResponse {
        turns = turns == null ? List.of() : List.copyOf(turns);
    }
}
