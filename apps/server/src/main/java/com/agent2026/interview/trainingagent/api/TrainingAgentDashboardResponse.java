package com.agent2026.interview.trainingagent.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record TrainingAgentDashboardResponse(
        boolean enabled,
        boolean degraded,
        String state,
        List<AbilityFocus> focusDimensions,
        RecommendationItem primaryRecommendation,
        List<RecommendationItem> alternatives,
        RecentProgress recentProgress,
        LocalDateTime generatedAt) {
    public TrainingAgentDashboardResponse {
        focusDimensions = focusDimensions == null ? List.of() : List.copyOf(focusDimensions);
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
    }

    public record RecommendationItem(long revision, String trainingType, String dimensionCode, String title,
                                     String reason, int estimatedMinutes, Map<String, Object> action,
                                     int evidenceCount) {
        public RecommendationItem {
            action = action == null ? Map.of() : Map.copyOf(action);
        }
    }

    public record AbilityFocus(String dimensionCode, String label, String sourceType, String abilityState,
                               int evidenceCount, LocalDateTime lastObservedAt) {}

    public record RecentProgress(String dimensionCode, String label, String sourceType, String abilityState,
                                 LocalDateTime lastObservedAt) {}
}
