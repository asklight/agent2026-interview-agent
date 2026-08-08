package com.agent2026.interview.identity.api;

import jakarta.validation.constraints.NotBlank;

public record AuthCredentialsRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
) {
}
