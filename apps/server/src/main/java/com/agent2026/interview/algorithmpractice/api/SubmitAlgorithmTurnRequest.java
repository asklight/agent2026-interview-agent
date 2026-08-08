package com.agent2026.interview.algorithmpractice.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SubmitAlgorithmTurnRequest(
        @NotBlank @Size(max = 64) String clientTurnId,
        @NotNull @Positive Long questionTurnId,
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotBlank @Size(max = 12000) String content,
        @Size(max = 32) String inputModality) {
}
