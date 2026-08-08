package com.agent2026.interview.interviewsimulation.domain;

public enum SimulationStageType {
    PROJECT, KNOWLEDGE, ALGORITHM, REPORTING, FINISHED;

    public SimulationStageType next() {
        return switch (this) {
            case PROJECT -> KNOWLEDGE;
            case KNOWLEDGE -> ALGORITHM;
            case ALGORITHM -> REPORTING;
            case REPORTING, FINISHED -> FINISHED;
        };
    }
}
