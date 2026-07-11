package com.agent2026.interview.projectdeepdive.interview.api;

import com.agent2026.interview.projectdeepdive.interview.domain.InterviewTurn;
import java.time.LocalDateTime;

public record PublicInterviewTurnResponse(Long turnId, int sequenceNo, String role, String turnType,
                                          String content, String inputModality,
                                          LocalDateTime startedAt, LocalDateTime endedAt,
                                          LocalDateTime createTime) {
    public static PublicInterviewTurnResponse from(InterviewTurn turn) {
        return new PublicInterviewTurnResponse(turn.id(), turn.sequenceNo(), turn.role(), turn.turnType(),
                turn.content(), turn.inputModality(), turn.startedAt(), turn.endedAt(), turn.createTime());
    }
}
