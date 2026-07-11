package com.agent2026.interview.projectdeepdive.interview.api;

import java.util.List;

public record ProjectInterviewSessionResponse(Long sessionId, String mode, String status,
                                              String conversationPhase, String currentProbeDimension,
                                              int completedProbeCount, int totalProbeCount,
                                              int maxFollowUpsPerClaim, String inputModality,
                                              List<PublicInterviewTurnResponse> turns) {
    public ProjectInterviewSessionResponse {
        turns = turns == null ? List.of() : List.copyOf(turns);
    }
}
