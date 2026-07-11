package com.agent2026.interview.projectdeepdive.interview.domain;

import java.time.LocalDateTime;

public record InterviewTurn(Long id, Long sessionId, int sequenceNo, String role, String turnType,
                            String content, String inputModality, Long parentTurnId, Long claimId,
                            String probeId, String probeDimension, String processingStatus,
                            LocalDateTime processingStartedAt, String clientTurnId,
                            LocalDateTime startedAt, LocalDateTime endedAt, LocalDateTime createTime) {
}
