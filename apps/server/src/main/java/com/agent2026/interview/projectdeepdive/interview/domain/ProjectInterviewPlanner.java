package com.agent2026.interview.projectdeepdive.interview.domain;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProjectInterviewPlanner {
    private static final List<String> REQUIRED_DIMENSIONS =
            List.of("OWNERSHIP", "AUTHENTICITY", "PRINCIPLE", "TRADEOFF");

    public List<PlannedProbe> createPlan(List<ProjectClaim> claims) {
        if (claims == null || claims.isEmpty()) {
            throw new IllegalArgumentException("confirmed claims are required");
        }
        List<PlannedProbe> probes = new ArrayList<>();
        for (int i = 0; i < REQUIRED_DIMENSIONS.size(); i++) {
            ProjectClaim claim = claims.get(Math.min(i, claims.size() - 1));
            String dimension = REQUIRED_DIMENSIONS.get(i);
            probes.add(new PlannedProbe("probe-" + (i + 1), claim.id(), dimension,
                    objective(dimension, claim.statement())));
        }
        return List.copyOf(probes);
    }

    private String objective(String dimension, String claim) {
        return switch (dimension) {
            case "OWNERSHIP" -> "澄清候选人本人负责的范围、动作和协作边界：" + claim;
            case "AUTHENTICITY" -> "验证项目过程细节和可核验证据：" + claim;
            case "PRINCIPLE" -> "验证关键技术原理与项目现象的联系：" + claim;
            default -> "验证方案选择、替代方案和工程代价：" + claim;
        };
    }
}
