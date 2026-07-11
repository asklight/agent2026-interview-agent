package com.agent2026.interview.projectdeepdive.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectProfile(
        Long id,
        String accessTokenHash,
        String sanitizedDescription,
        String projectName,
        String summary,
        List<String> techStack,
        List<String> responsibilities,
        List<String> metrics,
        List<String> architecture,
        List<String> uncertainties,
        ProjectAnalysisStatus analysisStatus,
        long version,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public ProjectProfile {
        techStack = techStack == null ? List.of() : List.copyOf(techStack);
        responsibilities = responsibilities == null ? List.of() : List.copyOf(responsibilities);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        architecture = architecture == null ? List.of() : List.copyOf(architecture);
        uncertainties = uncertainties == null ? List.of() : List.copyOf(uncertainties);
    }
}
