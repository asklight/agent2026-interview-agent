package com.agent2026.interview.shared.training;

import java.util.List;

public final class TrainingSourceTypes {
    public static final String KNOWLEDGE = "KNOWLEDGE";
    public static final String PROJECT_DEEP_DIVE = "PROJECT_DEEP_DIVE";
    public static final String ALGORITHM = "ALGORITHM";
    public static final String COMPREHENSIVE_SIMULATION = "COMPREHENSIVE_SIMULATION";
    public static final List<String> ORDERED = List.of(
            KNOWLEDGE, PROJECT_DEEP_DIVE, ALGORITHM, COMPREHENSIVE_SIMULATION);

    private TrainingSourceTypes() {
    }
}
