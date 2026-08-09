package com.agent2026.interview.trainingagent.domain;

import java.time.LocalDateTime;

public record AbilitySnapshot(
        AbilityDimension dimension,
        AbilityState state,
        double internalValue,
        double confidence,
        int strengthCount,
        int gapCount,
        int riskCount,
        int distinctSessionCount,
        LocalDateTime lastObservedAt) {
}
