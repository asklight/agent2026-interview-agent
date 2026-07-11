package com.agent2026.interview.projectdeepdive.api;

import com.agent2026.interview.projectdeepdive.application.PatchProjectProfileCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PatchProjectProfileRequest(
        @NotNull(message = "version 不能为空") @PositiveOrZero(message = "version 不合法") Long version,
        @Size(max = 255, message = "项目名称不能超过 255 个字符") String projectName,
        @Size(max = 4000, message = "项目摘要不能超过 4000 个字符") String summary,
        @Size(max = 50) List<@Size(max = 100) String> techStack,
        @Size(max = 50) List<@Size(max = 1000) String> responsibilities,
        @Size(max = 50) List<@Size(max = 1000) String> metrics,
        @Size(max = 50) List<@Size(max = 1000) String> architecture,
        @Size(max = 50) List<@Size(max = 1000) String> uncertainties,
        @Size(max = 30) List<@Valid ProjectClaimRequest> claims
) {
    @AssertTrue(message = "至少需要修改一个项目档案字段")
    public boolean isAnyChangePresent() {
        return projectName != null || summary != null || techStack != null || responsibilities != null
                || metrics != null || architecture != null || uncertainties != null || claims != null;
    }

    public PatchProjectProfileCommand toCommand() {
        return new PatchProjectProfileCommand(
                version,
                projectName,
                summary,
                techStack,
                responsibilities,
                metrics,
                architecture,
                uncertainties,
                claims == null ? null : claims.stream().map(ProjectClaimRequest::toDomain).toList()
        );
    }
}
