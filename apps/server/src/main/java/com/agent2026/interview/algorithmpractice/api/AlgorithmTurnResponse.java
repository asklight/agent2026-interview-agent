package com.agent2026.interview.algorithmpractice.api;

import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmTurnEntity;

import java.time.LocalDateTime;

public record AlgorithmTurnResponse(Long id, int sequence, String role, String stage, String content,
                                    String inputModality, Long parentTurnId, LocalDateTime createdAt) {
    public static AlgorithmTurnResponse from(AlgorithmTurnEntity turn) {
        return new AlgorithmTurnResponse(turn.getId(), turn.getSequenceNo(), turn.getRole(), turn.getStage(),
                turn.getContent(), turn.getInputModality(), turn.getParentTurnId(), turn.getCreateTime());
    }
}
