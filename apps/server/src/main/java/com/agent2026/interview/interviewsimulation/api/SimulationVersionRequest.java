package com.agent2026.interview.interviewsimulation.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SimulationVersionRequest(@NotNull @PositiveOrZero Long expectedVersion) {
}
