package com.agent2026.interview.projectdeepdive.api;

import com.agent2026.interview.projectdeepdive.application.CreatedProjectProfileResult;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectAnalysisStatus;

public record CreateProjectProfileResponse(
        Long profileId,
        String accessToken,
        ProjectAnalysisStatus analysisStatus,
        String sanitizedDescription,
        long version
) {
    public static CreateProjectProfileResponse from(CreatedProjectProfileResult result) {
        var profile = result.projectProfile().profile();
        return new CreateProjectProfileResponse(profile.id(), result.accessToken(), profile.analysisStatus(),
                profile.sanitizedDescription(), profile.version());
    }
}
