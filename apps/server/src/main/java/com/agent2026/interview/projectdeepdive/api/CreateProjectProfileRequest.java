package com.agent2026.interview.projectdeepdive.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectProfileRequest(
        @NotBlank(message = "项目经历不能为空")
        @Size(max = 20000, message = "项目经历不能超过 20000 个字符")
        String description
) {
}
