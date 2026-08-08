package com.agent2026.interview.algorithmpractice.application;

import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmEvaluationMapper;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmProblemMapper;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmSessionEntity;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmSessionMapper;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmTurnEntity;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmTurnMapper;
import com.agent2026.interview.shared.error.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlgorithmPersistenceServiceTest {
    private final AlgorithmProblemMapper problems = mock(AlgorithmProblemMapper.class);
    private final AlgorithmSessionMapper sessions = mock(AlgorithmSessionMapper.class);
    private final AlgorithmTurnMapper turns = mock(AlgorithmTurnMapper.class);
    private final AlgorithmEvaluationMapper evaluations = mock(AlgorithmEvaluationMapper.class);
    private final AlgorithmPersistenceService service = new AlgorithmPersistenceService(problems, sessions, turns,
            evaluations, new ObjectMapper(), Clock.fixed(Instant.parse("2026-08-08T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void rejectsCrossUserSessionAccess() {
        AlgorithmSessionEntity session = session(1L, 7L, 0L);
        when(sessions.selectById(1L)).thenReturn(session);

        assertThatThrownBy(() -> service.requireOwned(1L, 8L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(40311));
    }

    @Test
    void duplicateCompletedClientTurnIsIdempotent() {
        AlgorithmSessionEntity session = session(1L, 7L, 1L);
        AlgorithmTurnEntity existing = new AlgorithmTurnEntity();
        existing.setId(20L);
        existing.setProcessingStatus("COMPLETED");
        when(sessions.selectForUpdate(1L)).thenReturn(session);
        when(turns.selectByClientTurnId(1L, "client-1")).thenReturn(existing);

        var claim = service.claimCandidate(1L, 7L, 1L, "client-1", 10L, "answer", "TEXT");

        assertThat(claim.processingOwner()).isFalse();
        assertThat(claim.turn().getId()).isEqualTo(20L);
        verify(turns, never()).insert(any(AlgorithmTurnEntity.class));
    }

    @Test
    void staleVersionCannotRegisterANewAnswer() {
        when(sessions.selectForUpdate(1L)).thenReturn(session(1L, 7L, 2L));
        when(turns.selectByClientTurnId(1L, "client-2")).thenReturn(null);

        assertThatThrownBy(() -> service.claimCandidate(
                1L, 7L, 1L, "client-2", 10L, "answer", "TEXT"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(40941));
    }

    private AlgorithmSessionEntity session(Long id, Long userId, Long version) {
        AlgorithmSessionEntity session = new AlgorithmSessionEntity();
        session.setId(id);
        session.setUserId(userId);
        session.setStatus("IN_PROGRESS");
        session.setCurrentStage("CLARIFY");
        session.setVersion(version);
        return session;
    }
}
