package com.agent2026.interview.interviewsimulation.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitSimulationAnswerRequest(@NotBlank @Size(max=64) String clientTurnId,
                                            Long questionTurnId, Long expectedChildVersion,
                                            @NotBlank @Size(max=20000) String content,
                                            String inputModality) {}
