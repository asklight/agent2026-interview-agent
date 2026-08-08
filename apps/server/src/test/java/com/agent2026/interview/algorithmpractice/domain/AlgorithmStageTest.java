package com.agent2026.interview.algorithmpractice.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlgorithmStageTest {
    @Test
    void advancesThroughTheFixedInterviewSequence() {
        AlgorithmStage stage = AlgorithmStage.CLARIFY;
        stage = stage.next();
        assertThat(stage).isEqualTo(AlgorithmStage.BASELINE_SOLUTION);
        stage = stage.next().next().next().next();
        assertThat(stage).isEqualTo(AlgorithmStage.FOLLOW_UP);
        assertThat(stage.next()).isEqualTo(AlgorithmStage.FINISHED);
        assertThat(AlgorithmStage.FINISHED.next()).isEqualTo(AlgorithmStage.FINISHED);
    }

    @Test
    void followUpUsesTheEvaluatorsNaturalQuestion() {
        assertThat(AlgorithmStage.FOLLOW_UP.interviewerPrompt("如果内存受限呢？"))
                .isEqualTo("如果内存受限呢？");
    }
}
