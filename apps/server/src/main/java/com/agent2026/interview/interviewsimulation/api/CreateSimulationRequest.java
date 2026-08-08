package com.agent2026.interview.interviewsimulation.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSimulationRequest(@NotBlank @Size(max = 64) String clientRequestId,
                                      @NotNull @Positive Long projectProfileId,
                                      @NotNull @Positive Long algorithmProblemId,
                                      @NotBlank String knowledgeModule, String difficulty) {}
