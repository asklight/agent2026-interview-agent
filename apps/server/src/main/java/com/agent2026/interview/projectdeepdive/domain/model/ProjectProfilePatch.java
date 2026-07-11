package com.agent2026.interview.projectdeepdive.domain.model;

import java.util.List;

public record ProjectProfilePatch(
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
