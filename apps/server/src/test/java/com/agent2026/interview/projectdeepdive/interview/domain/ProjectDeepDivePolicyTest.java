package com.agent2026.interview.projectdeepdive.interview.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProjectDeepDivePolicyTest {
    private final ProjectDeepDivePolicy policy = new ProjectDeepDivePolicy();

    @Test void allowsFollowUpBelowLimit() {
        assertThat(policy.decide("FOLLOW_UP", 1, 3, 0, 4)).isEqualTo("FOLLOW_UP");
    }

    @Test void switchesDimensionAtFollowUpLimit() {
        assertThat(policy.decide("FOLLOW_UP", 3, 3, 0, 4)).isEqualTo("SWITCH_DIMENSION");
    }

    @Test void wrapsAfterLastProbe() {
        assertThat(policy.decide("SWITCH_DIMENSION", 0, 3, 3, 4)).isEqualTo("WRAP_UP");
    }
}
