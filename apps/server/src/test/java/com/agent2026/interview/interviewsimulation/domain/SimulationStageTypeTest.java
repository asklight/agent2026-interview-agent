package com.agent2026.interview.interviewsimulation.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationStageTypeTest {
    @Test
    void followsTheFixedInterviewOrder() {
        assertThat(SimulationStageType.PROJECT.next()).isEqualTo(SimulationStageType.KNOWLEDGE);
        assertThat(SimulationStageType.KNOWLEDGE.next()).isEqualTo(SimulationStageType.ALGORITHM);
        assertThat(SimulationStageType.ALGORITHM.next()).isEqualTo(SimulationStageType.REPORTING);
        assertThat(SimulationStageType.REPORTING.next()).isEqualTo(SimulationStageType.FINISHED);
        assertThat(SimulationStageType.FINISHED.next()).isEqualTo(SimulationStageType.FINISHED);
    }
}
