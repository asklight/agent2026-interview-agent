package com.agent2026.interview.projectdeepdive.api;

import com.agent2026.interview.projectdeepdive.application.ProjectProfileResult;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectAnalysisStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectProfileResponse(
        Long profileId,
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
        List<ProjectClaimResponse> claims,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public static ProjectProfileResponse from(ProjectProfileResult result) {
        var profile = result.profile();
        return new ProjectProfileResponse(
                profile.id(),
                profile.sanitizedDescription(),
                profile.projectName(),
                profile.summary(),
                profile.techStack(),
                profile.responsibilities(),
                profile.metrics(),
                profile.architecture(),
                profile.uncertainties(),
                profile.analysisStatus(),
                profile.version(),
                result.claims().stream().map(ProjectClaimResponse::from).toList(),
                profile.createTime(),
                profile.updateTime()
        );
    }
}
