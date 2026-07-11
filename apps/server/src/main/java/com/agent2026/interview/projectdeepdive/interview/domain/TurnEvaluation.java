package com.agent2026.interview.projectdeepdive.interview.domain;

public record TurnEvaluation(Long id, Long sessionId, Long candidateTurnId, String probeId,
                             TurnEvaluationResult result, String decision, String retrievalTraceJson) {
}
