package com.agent2026.interview.algorithmpractice.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAlgorithmSessionRequest(@NotNull @Positive Long problemId) {
}
