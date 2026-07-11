package com.agent2026.interview.projectdeepdive.application;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;

import java.util.List;

public record ProjectProfileResult(ProjectProfile profile, List<ProjectClaim> claims) {
    public ProjectProfileResult {
        claims = claims == null ? List.of() : List.copyOf(claims);
    }
}
