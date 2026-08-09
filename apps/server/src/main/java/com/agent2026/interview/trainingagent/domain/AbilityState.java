package com.agent2026.interview.trainingagent.domain;

public enum AbilityState {
    UNKNOWN,
    NEEDS_WORK,
    DEVELOPING,
    STABLE,
    STRONG;

    public boolean needsTraining() {
        return this == UNKNOWN || this == NEEDS_WORK || this == DEVELOPING;
    }

    public int priority() {
        return switch (this) {
            case NEEDS_WORK -> 3;
            case UNKNOWN -> 2;
            case DEVELOPING -> 1;
            case STABLE, STRONG -> 0;
        };
    }
}
