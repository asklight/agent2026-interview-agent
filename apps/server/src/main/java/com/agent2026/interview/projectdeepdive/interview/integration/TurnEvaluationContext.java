package com.agent2026.interview.projectdeepdive.interview.integration;

import com.agent2026.interview.projectdeepdive.interview.domain.InterviewTurn;
import com.agent2026.interview.projectdeepdive.interview.domain.PlannedProbe;
import java.util.List;

public record TurnEvaluationContext(String projectSummary, PlannedProbe probe,
                                    List<InterviewTurn> recentTurns, String candidateAnswer,
                                    List<String> retrievedKnowledge) {
    public TurnEvaluationContext {
        recentTurns = recentTurns == null ? List.of() : List.copyOf(recentTurns);
        retrievedKnowledge = retrievedKnowledge == null ? List.of() : List.copyOf(retrievedKnowledge);
    }
}
