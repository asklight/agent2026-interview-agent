package com.agent2026.interview.trainingagent.domain;

import java.util.List;

/** Recent completed training types ordered from newest to oldest. */
public record TrainingHistorySignal(List<String> recentTrainingTypes) {
    public TrainingHistorySignal {
        recentTrainingTypes = recentTrainingTypes == null ? List.of() : List.copyOf(recentTrainingTypes);
    }

    public static TrainingHistorySignal none() {
        return new TrainingHistorySignal(List.of());
    }

    public boolean isFatigued(String trainingType) {
        return recentTrainingTypes.size() >= 2
                && trainingType.equals(recentTrainingTypes.get(0))
                && trainingType.equals(recentTrainingTypes.get(1));
    }
}
