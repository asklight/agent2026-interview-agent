package com.agent2026.interview.projectdeepdive.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectClaim(
        Long id,
        Long projectProfileId,
        ProjectClaimType claimType,
        String statement,
        String sourceFragment,
        List<String> relatedTechnologies,
        List<String> expectedEvidence,
        ProjectClaimRiskLevel riskLevel,
        boolean confirmed,
        LocalDateTime createTime
) {
    public ProjectClaim {
        relatedTechnologies = relatedTechnologies == null ? List.of() : List.copyOf(relatedTechnologies);
        expectedEvidence = expectedEvidence == null ? List.of() : List.copyOf(expectedEvidence);
    }
}
