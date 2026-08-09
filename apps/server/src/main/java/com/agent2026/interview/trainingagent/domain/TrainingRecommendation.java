package com.agent2026.interview.trainingagent.domain;

import java.util.List;
import java.util.Map;

public record TrainingRecommendation(
        String state,
        Item primary,
        List<Item> alternatives) {
    public TrainingRecommendation {
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
    }

    public record Item(
            String trainingType,
            String dimensionCode,
            String title,
            String reason,
            int estimatedMinutes,
            Map<String, Object> action,
            List<Long> evidenceIds) {
        public Item {
            action = action == null ? Map.of() : Map.copyOf(action);
            evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        }
    }
}
