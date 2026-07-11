package com.agent2026.interview.projectdeepdive.interview.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitProjectTurnRequest(
        @NotBlank(message = "clientTurnId 不能为空") @Size(max = 64) String clientTurnId,
        @NotBlank(message = "回答内容不能为空") @Size(max = 20000) String content,
        String inputModality) {
}
