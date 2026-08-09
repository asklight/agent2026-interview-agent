package com.agent2026.interview.trainingagent.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record TrainingAgentDashboardResponse(
        boolean enabled,
        boolean degraded,
        String state,
        RecommendationItem primary,
        List<RecommendationItem> alternatives,
        List<AbilityFocus> focus,
        LocalDateTime generatedAt) {
    public TrainingAgentDashboardResponse {
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
        focus = focus == null ? List.of() : List.copyOf(focus);
    }

    public record RecommendationItem(String trainingType, String dimensionCode, String title, String reason,
                                     int estimatedMinutes, Map<String, Object> action, List<Long> evidenceIds) {
        public RecommendationItem {
            action = action == null ? Map.of() : Map.copyOf(action);
            evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        }
    }

    public record AbilityFocus(String dimensionCode, String label, String sourceType, String state,
                               double confidence, int gapCount, int riskCount, LocalDateTime lastObservedAt) {}
}
