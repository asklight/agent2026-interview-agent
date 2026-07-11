package com.agent2026.interview.projectdeepdive.application;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;

import java.util.List;

public record PatchProjectProfileCommand(
        long version,
        String projectName,
        String summary,
        List<String> techStack,
        List<String> responsibilities,
        List<String> metrics,
        List<String> architecture,
        List<String> uncertainties,
        List<ProjectClaim> claims
) {
}
