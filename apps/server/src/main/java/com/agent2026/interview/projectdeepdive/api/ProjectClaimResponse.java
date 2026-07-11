package com.agent2026.interview.projectdeepdive.api;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimType;

import java.util.List;

public record ProjectClaimResponse(
        Long claimId,
        ProjectClaimType claimType,
        String statement,
        String sourceFragment,
        List<String> relatedTechnologies,
        boolean confirmed
) {
    public static ProjectClaimResponse from(ProjectClaim claim) {
        return new ProjectClaimResponse(claim.id(), claim.claimType(), claim.statement(), claim.sourceFragment(),
                claim.relatedTechnologies(), claim.confirmed());
    }
}
