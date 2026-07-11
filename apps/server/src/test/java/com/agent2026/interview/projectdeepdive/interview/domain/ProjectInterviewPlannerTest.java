package com.agent2026.interview.projectdeepdive.interview.domain;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimRiskLevel;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ProjectInterviewPlannerTest {
    @Test void alwaysCoversRequiredDimensionsWithStableIds() {
        ProjectClaim claim = new ProjectClaim(7L, 1L, ProjectClaimType.RESPONSIBILITY, "负责缓存优化",
                "负责缓存优化", List.of("Redis"), List.of("个人动作"), ProjectClaimRiskLevel.MEDIUM, true, null);
        List<PlannedProbe> probes = new ProjectInterviewPlanner().createPlan(List.of(claim));
        assertThat(probes).extracting(PlannedProbe::dimension)
                .containsExactly("OWNERSHIP", "AUTHENTICITY", "PRINCIPLE", "TRADEOFF");
        assertThat(probes).extracting(PlannedProbe::probeId)
                .containsExactly("probe-1", "probe-2", "probe-3", "probe-4");
    }
}
