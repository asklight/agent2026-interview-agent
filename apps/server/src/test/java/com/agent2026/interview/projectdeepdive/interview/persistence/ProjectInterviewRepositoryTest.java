package com.agent2026.interview.projectdeepdive.interview.persistence;

import com.agent2026.interview.entity.InterviewSession;
import com.agent2026.interview.mapper.InterviewSessionMapper;
import com.agent2026.interview.projectdeepdive.interview.domain.PlannedProbe;
import com.agent2026.interview.projectdeepdive.interview.domain.TurnEvaluationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectInterviewRepositoryTest {
    private InterviewSessionMapper sessionMapper;
    private InterviewPlanMapper planMapper;
    private InterviewTurnMapper turnMapper;
    private TurnEvaluationMapper evaluationMapper;
    private ProjectInterviewRepository repository;

    @BeforeEach
    void setUp() {
        sessionMapper = mock(InterviewSessionMapper.class);
        planMapper = mock(InterviewPlanMapper.class);
        turnMapper = mock(InterviewTurnMapper.class);
        evaluationMapper = mock(TurnEvaluationMapper.class);
        repository = new ProjectInterviewRepository(sessionMapper, planMapper, turnMapper, evaluationMapper,
                new ObjectMapper());
    }

    @Test
    void duplicateFoundAfterSessionLockDoesNotOwnProcessing() {
        InterviewSession session = activeSession();
        InterviewTurnEntity duplicate = processingTurn(LocalDateTime.of(2026, 8, 2, 10, 0, 0, 123_000_000));
        when(sessionMapper.selectForUpdate(8L)).thenReturn(session);
        when(turnMapper.selectOne(any())).thenReturn(duplicate);

        var registration = repository.registerCandidate(8L, 0, "client-race", 10L,
                "answer", "TEXT", probe());

        assertThat(registration.processingOwner()).isFalse();
        assertThat(registration.turn().id()).isEqualTo(20L);
        verify(sessionMapper, never()).reserveTurn(8L, 0);
        verify(turnMapper, never()).insert(any(InterviewTurnEntity.class));
    }

    @Test
    void insertedCandidateOwnsAMillisecondPrecisionLease() {
        when(sessionMapper.selectForUpdate(8L)).thenReturn(activeSession());
        when(turnMapper.selectLatestInterviewer(8L)).thenReturn(interviewerTurn(10L));
        when(turnMapper.countProcessingCandidates(8L)).thenReturn(0);
        when(sessionMapper.reserveTurn(8L, 0)).thenReturn(1);
        when(turnMapper.maxSequence(8L)).thenReturn(1);

        var registration = repository.registerCandidate(8L, 0, "client-owner", 10L,
                "answer", "TEXT", probe());

        ArgumentCaptor<InterviewTurnEntity> inserted = ArgumentCaptor.forClass(InterviewTurnEntity.class);
        verify(turnMapper).insert(inserted.capture());
        assertThat(registration.processingOwner()).isTrue();
        assertThat(registration.turn().processingStartedAt().getNano() % 1_000_000).isZero();
        assertThat(inserted.getValue().getProcessingStartedAt())
                .isEqualTo(registration.turn().processingStartedAt());
        assertThat(inserted.getValue().getParentTurnId()).isEqualTo(10L);
    }

    @Test
    void differentTabCannotRegisterAnotherKeyWhileTheSameQuestionIsProcessing() {
        when(sessionMapper.selectForUpdate(8L)).thenReturn(activeSession());
        when(turnMapper.selectLatestInterviewer(8L)).thenReturn(interviewerTurn(10L));
        when(turnMapper.countProcessingCandidates(8L)).thenReturn(1);

        var registration = repository.registerCandidate(8L, 0, "second-tab", 10L,
                "second answer", "TEXT", probe());

        assertThat(registration).isNull();
        verify(sessionMapper, never()).reserveTurn(8L, 0);
        verify(turnMapper, never()).insert(any(InterviewTurnEntity.class));
    }

    @Test
    void staleQuestionCannotRegisterANewClientTurn() {
        when(sessionMapper.selectForUpdate(8L)).thenReturn(activeSession());
        when(turnMapper.selectLatestInterviewer(8L)).thenReturn(interviewerTurn(11L));

        var registration = repository.registerCandidate(8L, 0, "stale-tab", 10L,
                "stale answer", "TEXT", probe());

        assertThat(registration).isNull();
        verify(turnMapper, never()).countProcessingCandidates(8L);
        verify(sessionMapper, never()).reserveTurn(8L, 0);
        verify(turnMapper, never()).insert(any(InterviewTurnEntity.class));
    }

    @Test
    void newClientTurnWithoutQuestionCannotBeRegistered() {
        when(sessionMapper.selectForUpdate(8L)).thenReturn(activeSession());
        when(turnMapper.selectLatestInterviewer(8L)).thenReturn(interviewerTurn(10L));

        var registration = repository.registerCandidate(8L, 0, "missing-question", null,
                "answer", "TEXT", probe());

        assertThat(registration).isNull();
        verify(turnMapper, never()).countProcessingCandidates(8L);
        verify(turnMapper, never()).insert(any(InterviewTurnEntity.class));
    }

    @Test
    void oldLeaseCannotMarkANewLeaseRetryable() {
        LocalDateTime oldLease = LocalDateTime.of(2026, 8, 2, 10, 0, 0, 123_000_000);
        when(turnMapper.markRetryable(20L, oldLease)).thenReturn(0);

        assertThat(repository.markRetryable(20L, oldLease)).isFalse();
        verify(turnMapper).markRetryable(20L, oldLease);
    }

    @Test
    void oldLeaseCannotStartCompletionWrites() {
        LocalDateTime oldLease = LocalDateTime.of(2026, 8, 2, 10, 0, 0, 123_000_000);
        LocalDateTime newLease = oldLease.plusMinutes(3);
        when(sessionMapper.selectForUpdate(8L)).thenReturn(activeSession());
        when(turnMapper.selectById(20L)).thenReturn(processingTurn(newLease));

        var completed = repository.complete(8L, 20L, oldLease, probe(), evaluation(), "FOLLOW_UP",
                probe(), "next question", false);

        assertThat(completed).isNull();
        verifyNoInteractions(evaluationMapper);
        verify(turnMapper, never()).insert(any(InterviewTurnEntity.class));
        verify(turnMapper, never()).markCompleted(any(), any(), any());
    }

    @Test
    void finalLeaseCasFailureThrowsSoTransactionalWritesRollBack() throws Exception {
        LocalDateTime lease = LocalDateTime.of(2026, 8, 2, 10, 0, 0, 123_000_000);
        when(sessionMapper.selectForUpdate(8L)).thenReturn(activeSession());
        when(turnMapper.selectById(20L)).thenReturn(processingTurn(lease));
        when(turnMapper.maxSequence(8L)).thenReturn(2);
        when(turnMapper.markCompleted(any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> repository.complete(8L, 20L, lease, probe(), evaluation(), "FOLLOW_UP",
                probe(), "next question", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lease changed");

        verify(turnMapper).markCompleted(eq(20L), eq(lease), any(LocalDateTime.class));
        assertThat(ProjectInterviewRepository.class
                .getMethod("complete", Long.class, Long.class, LocalDateTime.class, PlannedProbe.class,
                        TurnEvaluationResult.class, String.class, PlannedProbe.class, String.class, boolean.class)
                .getAnnotation(Transactional.class))
                .isNotNull();
    }

    @Test
    void mapperFencingUpdatesRequireTheExpectedLease() throws Exception {
        Update retrySql = InterviewTurnMapper.class
                .getMethod("markRetryable", Long.class, LocalDateTime.class)
                .getAnnotation(Update.class);
        Update completeSql = InterviewTurnMapper.class
                .getMethod("markCompleted", Long.class, LocalDateTime.class, LocalDateTime.class)
                .getAnnotation(Update.class);

        assertThat(String.join(" ", retrySql.value()))
                .contains("processing_started_at=#{expectedLeaseStartedAt}");
        assertThat(String.join(" ", completeSql.value()))
                .contains("processing_started_at=#{expectedLeaseStartedAt}");
    }

    @Test
    void latestQuestionQuerySelectsTheLastInterviewerTurn() throws Exception {
        Select questionSql = InterviewTurnMapper.class
                .getMethod("selectLatestInterviewer", Long.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", questionSql.value());

        assertThat(sql)
                .contains("session_id=#{sessionId}", "role='INTERVIEWER'", "ORDER BY sequence_no DESC LIMIT 1");
    }

    private InterviewSession activeSession() {
        InterviewSession session = new InterviewSession();
        session.setId(8L);
        session.setStatus("IN_PROGRESS");
        session.setVersion(0L);
        session.setFollowUpCount(0);
        session.setCompletedQuestionCount(0);
        session.setQuestionCount(1);
        return session;
    }

    private InterviewTurnEntity processingTurn(LocalDateTime lease) {
        InterviewTurnEntity turn = new InterviewTurnEntity();
        turn.setId(20L);
        turn.setSessionId(8L);
        turn.setSequenceNo(2);
        turn.setRole("CANDIDATE");
        turn.setTurnType("ANSWER");
        turn.setContent("answer");
        turn.setInputModality("TEXT");
        turn.setClaimId(4L);
        turn.setProbeId("probe-1");
        turn.setProbeDimension("OWNERSHIP");
        turn.setProcessingStatus("PROCESSING");
        turn.setProcessingStartedAt(lease);
        turn.setClientTurnId("client-race");
        turn.setStartedAt(lease);
        turn.setCreateTime(lease);
        return turn;
    }

    private InterviewTurnEntity interviewerTurn(Long id) {
        InterviewTurnEntity turn = new InterviewTurnEntity();
        turn.setId(id);
        turn.setSessionId(8L);
        turn.setSequenceNo(1);
        turn.setRole("INTERVIEWER");
        turn.setTurnType("OPENING");
        turn.setContent("question");
        turn.setInputModality("TEXT");
        turn.setProcessingStatus("COMPLETED");
        return turn;
    }

    private PlannedProbe probe() {
        return new PlannedProbe("probe-1", 4L, "OWNERSHIP", "ownership");
    }

    private TurnEvaluationResult evaluation() {
        return new TurnEvaluationResult(Map.of("ownership", 70), List.of(), List.of(), List.of(), List.of(),
                List.of(), "FOLLOW_UP", "next question", "hash", false);
    }
}
