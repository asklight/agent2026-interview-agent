package com.agent2026.interview.projectdeepdive.api;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProjectClaimRequest(
        @NotNull(message = "claimId 不能为空") Long claimId,
        @NotNull(message = "项目声明类型不能为空") ProjectClaimType claimType,
        @NotBlank(message = "项目声明不能为空") @Size(max = 2000) String statement,
        @NotBlank(message = "项目声明来源片段不能为空") @Size(max = 2000) String sourceFragment,
        @Size(max = 50) List<@NotBlank @Size(max = 100) String> relatedTechnologies
) {
    public ProjectClaim toDomain() {
        return new ProjectClaim(claimId, null, claimType, statement, sourceFragment,
                relatedTechnologies, List.of(), null, false, null);
    }
}
