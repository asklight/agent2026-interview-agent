package com.agent2026.interview.projectdeepdive.interview.application;

import com.agent2026.interview.entity.InterviewSession;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectAnalysisStatus;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimType;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimRiskLevel;
import com.agent2026.interview.param.CreateInterviewSessionParam;
import com.agent2026.interview.projectdeepdive.domain.port.ProjectProfileRepository;
import com.agent2026.interview.projectdeepdive.interview.api.SubmitProjectTurnRequest;
import com.agent2026.interview.projectdeepdive.interview.domain.InterviewPlan;
import com.agent2026.interview.projectdeepdive.interview.domain.InterviewTurn;
import com.agent2026.interview.projectdeepdive.interview.domain.PlannedProbe;
import com.agent2026.interview.projectdeepdive.interview.domain.ProjectDeepDivePolicy;
import com.agent2026.interview.projectdeepdive.interview.domain.ProjectInterviewPlanner;
import com.agent2026.interview.projectdeepdive.interview.integration.TurnEvaluator;
import com.agent2026.interview.projectdeepdive.interview.knowledge.VectorRetrievalService;
import com.agent2026.interview.projectdeepdive.interview.persistence.ProjectInterviewRepository;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.agent2026.interview.shared.security.ResourceTokenService;
import com.agent2026.interview.projectdeepdive.report.application.ProjectInterviewReportService;
import com.agent2026.interview.projectdeepdive.report.api.ProjectInterviewReportResponse;
import com.agent2026.interview.projectdeepdive.interview.integration.TurnEvaluationContext;
import com.agent2026.interview.projectdeepdive.interview.domain.TurnEvaluationResult;
import com.agent2026.interview.projectdeepdive.interview.knowledge.RetrievalContext;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DeepDiveInterviewApplicationServiceTest {
    private ProjectProfileRepository profiles;
    private ProjectInterviewRepository interviews;
    private TurnEvaluator evaluator;
    private VectorRetrievalService retrieval;
    private ProjectInterviewReportService reportService;
    private DeepDiveInterviewApplicationService service;
    private InterviewSession session;
    private InterviewPlan plan;

    @BeforeEach void setUp() {
        profiles = mock(ProjectProfileRepository.class); interviews = mock(ProjectInterviewRepository.class);
        evaluator = mock(TurnEvaluator.class); ResourceTokenService tokens = mock(ResourceTokenService.class);
        retrieval = mock(VectorRetrievalService.class);
        reportService = mock(ProjectInterviewReportService.class);
        service = new DeepDiveInterviewApplicationService(profiles, interviews, new ProjectInterviewPlanner(),
                new ProjectDeepDivePolicy(), retrieval, evaluator, tokens, reportService);
        ProjectProfile profile = new ProjectProfile(3L, "hash", "desc", "p", "summary", List.of(), List.of(),
                List.of(), List.of(), List.of(), ProjectAnalysisStatus.READY, 1, null, null);
        when(profiles.findById(3L)).thenReturn(Optional.of(profile)); when(tokens.matches("token", "hash")).thenReturn(true);
        session = new InterviewSession(); session.setId(8L); session.setMode("PROJECT_DEEP_DIVE");
        session.setProjectProfileId(3L); session.setStatus("IN_PROGRESS"); session.setCurrentClaimId(4L);
        session.setCurrentProbeDimension("OWNERSHIP"); session.setCompletedQuestionCount(0); session.setMaxFollowUpCount(3);
        session.setInputModality("TEXT"); session.setVersion(0L);
        PlannedProbe probe = new PlannedProbe("probe-1", 4L, "OWNERSHIP", "ownership");
        plan = new InterviewPlan(1L, 8L, "{}", List.of(probe), 1, "ACTIVE");
        when(interviews.findSession(8L)).thenReturn(Optional.of(session)); when(interviews.findPlan(8L)).thenReturn(plan);
        when(interviews.findTurns(8L)).thenReturn(List.of());
    }

    @Test void completedClientTurnIsReturnedWithoutCallingLlmAgain() {
        InterviewTurn completed = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "answer", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "COMPLETED", LocalDateTime.now(), "client-1",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(interviews.findByClientTurnId(8L, "client-1")).thenReturn(Optional.of(completed));
        service.submit(8L, "token", new SubmitProjectTurnRequest("client-1", 999L, "answer", "TEXT"));
        verifyNoInteractions(evaluator);
    }

    @Test void finalCompletedTurnCanBeReconciledAfterItsResponseWasLost() {
        session.setStatus("FINISHED");
        session.setConversationPhase("WRAP_UP");
        InterviewTurn completed = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "final answer", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "COMPLETED", LocalDateTime.now(), "final-client",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(interviews.findByClientTurnId(8L, "final-client")).thenReturn(Optional.of(completed));
        when(interviews.findTurns(8L)).thenReturn(List.of(completed));

        var response = service.submit(8L, "token",
                new SubmitProjectTurnRequest("final-client", 999L, "replayed payload", "VOICE_TRANSCRIPT"));

        assertThat(response.status()).isEqualTo("FINISHED");
        assertThat(response.turns()).hasSize(1);
        verify(reportService).generateIfAbsent(8L);
        verifyNoInteractions(retrieval, evaluator);
        verify(interviews, never()).registerCandidate(anyLong(), anyLong(), anyString(), anyLong(),
                anyString(), anyString(), any());
    }

    @Test void replayRetriesReportGenerationAfterTheFirstCompensationFailed() {
        session.setStatus("FINISHED");
        session.setConversationPhase("WRAP_UP");
        InterviewTurn completed = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "final answer", "TEXT",
                10L, 4L, "probe-1", "OWNERSHIP", "COMPLETED", LocalDateTime.now(), "final-client",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(interviews.findByClientTurnId(8L, "final-client")).thenReturn(Optional.of(completed));
        when(interviews.findTurns(8L)).thenReturn(List.of(completed));
        when(reportService.generateIfAbsent(8L))
                .thenThrow(new IllegalStateException("report storage unavailable"))
                .thenReturn(null);

        SubmitProjectTurnRequest replay = new SubmitProjectTurnRequest(
                "final-client", 999L, "replayed payload", "TEXT");
        assertThatThrownBy(() -> service.submit(8L, "token", replay))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("report storage unavailable");

        var response = service.submit(8L, "token", replay);

        assertThat(response.status()).isEqualTo("FINISHED");
        verify(reportService, times(2)).generateIfAbsent(8L);
        verifyNoInteractions(retrieval, evaluator);
    }

    @Test void newClientTurnIsStillRejectedAfterSessionFinished() {
        session.setStatus("FINISHED");
        when(interviews.findByClientTurnId(8L, "new-client")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(8L, "token",
                new SubmitProjectTurnRequest("new-client", 10L, "new answer", "TEXT")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INTERVIEW_SESSION_FINISHED);

        verifyNoInteractions(retrieval, evaluator);
        verify(interviews, never()).registerCandidate(anyLong(), anyLong(), anyString(), anyLong(),
                anyString(), anyString(), any());
    }

    @Test void freshProcessingTurnReturnsStableProcessingError() {
        InterviewTurn processing = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "answer", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "PROCESSING", LocalDateTime.now(), "client-1",
                LocalDateTime.now(), null, LocalDateTime.now());
        when(interviews.findByClientTurnId(8L, "client-1")).thenReturn(Optional.of(processing));
        assertThatThrownBy(() -> service.submit(8L, "token",
                new SubmitProjectTurnRequest("client-1", 999L, "answer", "TEXT")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INTERVIEW_TURN_PROCESSING);
        verifyNoInteractions(evaluator);
    }

    @Test void concurrentFirstSubmissionWithoutProcessingOwnershipNeverCallsLlm() {
        InterviewTurn processing = processingCandidate("client-race", "first answer");
        when(interviews.findByClientTurnId(8L, "client-race")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(eq(8L), anyLong(), eq("client-race"), eq(10L),
                anyString(), eq("TEXT"), any()))
                .thenReturn(new ProjectInterviewRepository.CandidateRegistration(processing, false));

        assertThatThrownBy(() -> service.submit(8L, "token",
                new SubmitProjectTurnRequest("client-race", 10L, "first answer", "TEXT")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INTERVIEW_TURN_PROCESSING);

        verifyNoInteractions(retrieval, evaluator);
        verify(interviews, never()).complete(anyLong(), anyLong(), any(), any(), any(), anyString(),
                any(), anyString(), anyBoolean());
    }

    @Test void concurrentFirstSubmissionCompletedByOwnerReturnsWithoutCallingLlm() {
        InterviewTurn completed = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "first answer", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "COMPLETED", LocalDateTime.now(), "client-race",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(interviews.findByClientTurnId(8L, "client-race")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(eq(8L), anyLong(), eq("client-race"), eq(10L),
                anyString(), eq("TEXT"), any()))
                .thenReturn(new ProjectInterviewRepository.CandidateRegistration(completed, false));

        service.submit(8L, "token", new SubmitProjectTurnRequest(
                "client-race", 10L, "first answer", "TEXT"));

        verifyNoInteractions(retrieval, evaluator);
    }

    @Test void retryAlwaysEvaluatesFirstPersistedContent() {
        InterviewTurn retryable = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "首次回答", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "RETRYABLE_FAILED", LocalDateTime.now(), "client-1",
                LocalDateTime.now(), null, LocalDateTime.now());
        InterviewTurn processing = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "首次回答", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "PROCESSING", LocalDateTime.now(), "client-1",
                LocalDateTime.now(), null, LocalDateTime.now());
        when(interviews.findByClientTurnId(8L, "client-1"))
                .thenReturn(Optional.of(retryable), Optional.of(processing), Optional.of(processing));
        when(interviews.claimRetry(eq(20L), any())).thenReturn(true);
        when(retrieval.retrieve(anyString())).thenReturn(new RetrievalContext(List.of(), false));
        when(evaluator.evaluate(any())).thenReturn(new TurnEvaluationResult(Map.of("ownership", 70), List.of(),
                List.of(), List.of(), List.of(), List.of(), "WRAP_UP", "最后总结一下", "hash", false));
        when(interviews.complete(anyLong(), anyLong(), any(), any(), any(), anyString(), any(), anyString(), anyBoolean()))
                .thenReturn(new InterviewTurn(21L, 8L, 3, "INTERVIEWER", "CLOSING", "最后总结一下", "TEXT",
                        20L, 4L, "probe-1", "OWNERSHIP", "COMPLETED", null, null, null, null, null));

        service.submit(8L, "token", new SubmitProjectTurnRequest(
                "client-1", 999L, "篡改后的回答", "TEXT"));

        ArgumentCaptor<TurnEvaluationContext> context = ArgumentCaptor.forClass(TurnEvaluationContext.class);
        verify(evaluator).evaluate(context.capture());
        org.assertj.core.api.Assertions.assertThat(context.getValue().candidateAnswer()).isEqualTo("首次回答");
        verify(retrieval).retrieve(contains("首次回答"));
        verify(interviews).complete(eq(8L), eq(20L), eq(processing.processingStartedAt()), any(), any(),
                anyString(), any(), anyString(), anyBoolean());
    }

    @Test void differentClientTurnCannotBypassUnresolvedProcessingTurn() {
        when(interviews.findByClientTurnId(8L, "client-2")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(eq(8L), anyLong(), eq("client-2"), eq(10L),
                anyString(), eq("TEXT"), any()))
                .thenReturn(null);
        assertThatThrownBy(() -> service.submit(8L, "token",
                new SubmitProjectTurnRequest("client-2", 10L, "第二个回答", "TEXT")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INTERVIEW_STATE_CONFLICT);
        verifyNoInteractions(evaluator);
    }

    @Test void newClientTurnForAnOldQuestionReturnsStateConflict() {
        when(interviews.findByClientTurnId(8L, "stale-tab")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(eq(8L), anyLong(), eq("stale-tab"), eq(9L),
                anyString(), eq("TEXT"), any()))
                .thenReturn(null);

        assertThatThrownBy(() -> service.submit(8L, "token",
                new SubmitProjectTurnRequest("stale-tab", 9L, "旧题回答", "TEXT")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INTERVIEW_STATE_CONFLICT);

        verifyNoInteractions(retrieval, evaluator);
    }

    @Test void answerToWrapUpQuestionFinishesAndGeneratesReport() {
        session.setConversationPhase("WRAP_UP");
        InterviewTurn candidate = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "我的反思", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "PROCESSING", LocalDateTime.now(), "client-finish",
                LocalDateTime.now(), null, LocalDateTime.now());
        when(interviews.findByClientTurnId(8L, "client-finish")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(eq(8L), anyLong(), eq("client-finish"), eq(10L),
                anyString(), eq("TEXT"), any()))
                .thenReturn(new ProjectInterviewRepository.CandidateRegistration(candidate, true));
        when(retrieval.retrieve(anyString())).thenReturn(new RetrievalContext(List.of(), false));
        when(evaluator.evaluate(any())).thenReturn(new TurnEvaluationResult(Map.of("ownership", 75), List.of(),
                List.of(), List.of(), List.of(), List.of(), "WRAP_UP", "再总结", "hash", false));
        when(interviews.complete(anyLong(), anyLong(), any(), any(), any(), eq("FINISH"), any(), contains("结束"), anyBoolean()))
                .thenReturn(new InterviewTurn(21L, 8L, 3, "INTERVIEWER", "CLOSING", "结束", "TEXT",
                        20L, 4L, "probe-1", "OWNERSHIP", "COMPLETED", null, null, null, null, null));

        service.submit(8L, "token", new SubmitProjectTurnRequest(
                "client-finish", 10L, "我的反思", "TEXT"));

        verify(interviews).complete(anyLong(), anyLong(), any(), any(), any(), eq("FINISH"), any(), contains("结束"), anyBoolean());
        verify(reportService).generateIfAbsent(8L);
    }

    @Test void losingFinishDecisionDoesNotGenerateReportWhenWinnerKeptSessionInProgress() {
        session.setConversationPhase("WRAP_UP");
        InterviewTurn candidate = processingCandidate("client-loser", "我的反思");
        InterviewTurn completedByWinner = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "我的反思", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "COMPLETED", candidate.processingStartedAt().plusSeconds(1),
                "client-loser", candidate.startedAt(), LocalDateTime.now(), candidate.createTime());
        when(interviews.findByClientTurnId(8L, "client-loser"))
                .thenReturn(Optional.empty(), Optional.of(completedByWinner));
        when(interviews.registerCandidate(anyLong(), anyLong(), eq("client-loser"), eq(10L),
                anyString(), anyString(), any()))
                .thenReturn(new ProjectInterviewRepository.CandidateRegistration(candidate, true));
        when(retrieval.retrieve(anyString())).thenReturn(new RetrievalContext(List.of(), false));
        when(evaluator.evaluate(any())).thenReturn(evaluation("WRAP_UP"));

        service.submit(8L, "token", new SubmitProjectTurnRequest(
                "client-loser", 10L, "我的反思", "TEXT"));

        verify(interviews).complete(anyLong(), anyLong(), any(), any(), any(), eq("FINISH"), any(), anyString(),
                anyBoolean());
        verify(reportService, never()).generateIfAbsent(anyLong());
    }

    @Test void losingCompletionGeneratesReportOnlyWhenPersistedWinnerFinishedSession() {
        session.setConversationPhase("WRAP_UP");
        InterviewSession finished = finishedSession();
        InterviewTurn candidate = processingCandidate("client-loser", "我的反思");
        InterviewTurn completedByWinner = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "我的反思", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "COMPLETED", candidate.processingStartedAt().plusSeconds(1),
                "client-loser", candidate.startedAt(), LocalDateTime.now(), candidate.createTime());
        when(interviews.findSession(8L)).thenReturn(Optional.of(session), Optional.of(session),
                Optional.of(finished), Optional.of(finished));
        when(interviews.findByClientTurnId(8L, "client-loser"))
                .thenReturn(Optional.empty(), Optional.of(completedByWinner));
        when(interviews.registerCandidate(anyLong(), anyLong(), eq("client-loser"), eq(10L),
                anyString(), anyString(), any()))
                .thenReturn(new ProjectInterviewRepository.CandidateRegistration(candidate, true));
        when(retrieval.retrieve(anyString())).thenReturn(new RetrievalContext(List.of(), false));
        when(evaluator.evaluate(any())).thenReturn(evaluation("WRAP_UP"));

        service.submit(8L, "token", new SubmitProjectTurnRequest(
                "client-loser", 10L, "我的反思", "TEXT"));

        verify(reportService).generateIfAbsent(8L);
    }

    @Test void firstCoreClaimForcesProcessEvidenceFollowUp() {
        InterviewTurn candidate = processingCandidate("forced-1", "回答");
        when(interviews.findByClientTurnId(8L, "forced-1")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(anyLong(), anyLong(), eq("forced-1"), eq(10L),
                anyString(), anyString(), any()))
                .thenReturn(new ProjectInterviewRepository.CandidateRegistration(candidate, true));
        when(retrieval.retrieve(anyString())).thenReturn(new RetrievalContext(List.of(), false));
        when(evaluator.evaluate(any())).thenReturn(evaluation("SWITCH_DIMENSION"));
        when(interviews.complete(anyLong(), anyLong(), any(), any(), any(), eq("FOLLOW_UP"), any(), contains("按时间顺序"), anyBoolean()))
                .thenReturn(candidate);
        service.submit(8L, "token", new SubmitProjectTurnRequest("forced-1", 10L, "回答", "TEXT"));
        verify(interviews).complete(anyLong(), anyLong(), any(), any(), any(), eq("FOLLOW_UP"), any(), contains("按时间顺序"), anyBoolean());
    }

    @Test void secondCoreClaimFollowUpForcesTechnicalTradeoffEvidence() {
        session.setFollowUpCount(1);
        InterviewTurn candidate = processingCandidate("forced-2", "回答");
        when(interviews.findByClientTurnId(8L, "forced-2")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(anyLong(), anyLong(), eq("forced-2"), eq(10L),
                anyString(), anyString(), any()))
                .thenReturn(new ProjectInterviewRepository.CandidateRegistration(candidate, true));
        when(retrieval.retrieve(anyString())).thenReturn(new RetrievalContext(List.of(), false));
        when(evaluator.evaluate(any())).thenReturn(evaluation("SWITCH_DIMENSION"));
        when(interviews.complete(anyLong(), anyLong(), any(), any(), any(), eq("FOLLOW_UP"), any(), contains("替代方案"), anyBoolean()))
                .thenReturn(candidate);
        service.submit(8L, "token", new SubmitProjectTurnRequest("forced-2", 10L, "回答", "TEXT"));
        verify(interviews).complete(anyLong(), anyLong(), any(), any(), any(), eq("FOLLOW_UP"), any(), contains("替代方案"), anyBoolean());
    }

    @Test void activeProcessingTurnBlocksFinishAndReportGeneration() {
        when(interviews.finish(8L)).thenReturn(null);
        assertThatThrownBy(() -> service.finish(8L, "token"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INTERVIEW_TURN_PROCESSING);
        verifyNoInteractions(reportService);
    }

    @Test void finishedSessionReportReadSelfHealsBeforeReturningReport() {
        session.setStatus("FINISHED");
        ProjectInterviewReportResponse report = mock(ProjectInterviewReportResponse.class);
        when(reportService.get(8L)).thenReturn(report);

        assertThat(service.getReport(8L, "token")).isSameAs(report);

        InOrder calls = inOrder(reportService);
        calls.verify(reportService).generateIfAbsent(8L);
        calls.verify(reportService).get(8L);
    }

    @Test void inProgressSessionReportReadKeepsExistingNotReadyPath() {
        service.getReport(8L, "token");

        verify(reportService, never()).generateIfAbsent(anyLong());
        verify(reportService).get(8L);
    }

    @Test void publicResponseShowsProcessingStateWithoutExposingPrivateTurnState() {
        InterviewTurn processing = processingCandidate("pending-client", "待处理回答");
        when(interviews.findTurns(8L)).thenReturn(List.of(processing));

        var response = service.getTurns(8L, "token");

        assertThat(response.turnState()).isEqualTo("PROCESSING");
        assertThat(response.turns()).hasSize(1);
    }

    @Test void staleProcessingTurnIsPubliclyRetryable() {
        InterviewTurn stale = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "待处理回答", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "PROCESSING", LocalDateTime.now().minusMinutes(3),
                "pending-client", LocalDateTime.now(), null, LocalDateTime.now());
        when(interviews.findTurns(8L)).thenReturn(List.of(stale));

        assertThat(service.getTurns(8L, "token").turnState()).isEqualTo("RETRYABLE_ERROR");
    }

    @Test void retryPendingSupportsLegacyTurnWithoutParentQuestion() {
        InterviewTurn retryable = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "服务端回答",
                "VOICE_TRANSCRIPT", null, 4L, "probe-1", "OWNERSHIP", "RETRYABLE_FAILED",
                LocalDateTime.now(), "server-client-id", LocalDateTime.now(), null, LocalDateTime.now());
        InterviewTurn processing = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "服务端回答",
                "VOICE_TRANSCRIPT", null, 4L, "probe-1", "OWNERSHIP", "PROCESSING",
                LocalDateTime.now(), "server-client-id", LocalDateTime.now(), null, LocalDateTime.now());
        when(interviews.findPendingCandidate(8L)).thenReturn(Optional.of(retryable));
        when(interviews.findByClientTurnId(8L, "server-client-id"))
                .thenReturn(Optional.of(retryable), Optional.of(processing), Optional.of(processing));
        when(interviews.claimRetry(eq(20L), any())).thenReturn(true);
        when(retrieval.retrieve(anyString())).thenReturn(new RetrievalContext(List.of(), false));
        when(evaluator.evaluate(any())).thenReturn(evaluation("WRAP_UP"));
        when(interviews.complete(anyLong(), anyLong(), any(), any(), any(), anyString(), any(), anyString(), anyBoolean()))
                .thenReturn(processing);

        service.retryPending(8L, "token");

        ArgumentCaptor<TurnEvaluationContext> context = ArgumentCaptor.forClass(TurnEvaluationContext.class);
        verify(evaluator).evaluate(context.capture());
        assertThat(context.getValue().candidateAnswer()).isEqualTo("服务端回答");
        verify(interviews, times(2)).findByClientTurnId(8L, "server-client-id");
    }

    @Test void retryPendingBuildsRecoveryRequestFromStoredParentQuestion() {
        InterviewTurn pending = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "服务端回答",
                "VOICE_TRANSCRIPT", 10L, 4L, "probe-1", "OWNERSHIP", "RETRYABLE_FAILED",
                LocalDateTime.now(), "server-client-id", LocalDateTime.now(), null, LocalDateTime.now());
        when(interviews.findPendingCandidate(8L)).thenReturn(Optional.of(pending));
        DeepDiveInterviewApplicationService recoveringService = spy(service);
        doReturn(null).when(recoveringService).submit(eq(8L), eq("token"), any(SubmitProjectTurnRequest.class));

        recoveringService.retryPending(8L, "token");

        ArgumentCaptor<SubmitProjectTurnRequest> request = ArgumentCaptor.forClass(SubmitProjectTurnRequest.class);
        verify(recoveringService).submit(eq(8L), eq("token"), request.capture());
        assertThat(request.getValue().clientTurnId()).isEqualTo("server-client-id");
        assertThat(request.getValue().questionTurnId()).isEqualTo(10L);
        assertThat(request.getValue().content()).isEqualTo("服务端回答");
        assertThat(request.getValue().inputModality()).isEqualTo("VOICE_TRANSCRIPT");
    }

    @Test void evaluatorFailureMarksOnlyTheOwnedLeaseRetryable() {
        LocalDateTime lease = LocalDateTime.of(2026, 8, 2, 10, 0, 0, 123_000_000);
        InterviewTurn candidate = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "回答", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "PROCESSING", lease, "lease-owner",
                lease, null, lease);
        when(interviews.findByClientTurnId(8L, "lease-owner")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(anyLong(), anyLong(), eq("lease-owner"), eq(10L),
                anyString(), anyString(), any()))
                .thenReturn(new ProjectInterviewRepository.CandidateRegistration(candidate, true));
        when(retrieval.retrieve(anyString())).thenReturn(new RetrievalContext(List.of(), false));
        when(evaluator.evaluate(any())).thenThrow(new IllegalStateException("upstream failed"));

        BusinessException failure = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> service.submit(8L, "token",
                        new SubmitProjectTurnRequest("lease-owner", 10L, "回答", "TEXT")),
                BusinessException.class);

        assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);
        assertThat(failure.getMessage())
                .isEqualTo("面试官暂时无法完成评价，可以安全重试上一条回答")
                .doesNotContain("clientTurnId");

        verify(interviews).markRetryable(20L, lease);
    }

    @Test void projectInterviewRejectsFollowUpLimitBelowTwo() {
        when(profiles.findClaims(3L)).thenReturn(List.of(new ProjectClaim(4L, 3L, ProjectClaimType.RESPONSIBILITY,
                "负责核心模块", "负责核心模块", List.of(), List.of(), ProjectClaimRiskLevel.MEDIUM, true, null)));
        CreateInterviewSessionParam param = new CreateInterviewSessionParam();
        param.setMode("PROJECT_DEEP_DIVE"); param.setProjectProfileId(3L); param.setMaxFollowUpsPerClaim(1);
        assertThatThrownBy(() -> service.create(param, "token"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
        verify(interviews, never()).create(any(), anyList(), anyList(), anyInt(), anyString());
    }

    private InterviewTurn processingCandidate(String clientId, String content) {
        return new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", content, "TEXT", null, 4L,
                "probe-1", "OWNERSHIP", "PROCESSING", LocalDateTime.now(), clientId, LocalDateTime.now(), null, LocalDateTime.now());
    }

    private InterviewSession finishedSession() {
        InterviewSession finished = new InterviewSession();
        finished.setId(8L);
        finished.setMode("PROJECT_DEEP_DIVE");
        finished.setProjectProfileId(3L);
        finished.setStatus("FINISHED");
        finished.setConversationPhase("WRAP_UP");
        finished.setCurrentClaimId(4L);
        finished.setCurrentProbeDimension("OWNERSHIP");
        finished.setCompletedQuestionCount(1);
        finished.setMaxFollowUpCount(3);
        finished.setInputModality("TEXT");
        finished.setVersion(2L);
        return finished;
    }

    private TurnEvaluationResult evaluation(String decision) {
        return new TurnEvaluationResult(Map.of("ownership", 70), List.of(), List.of(), List.of(), List.of(),
                List.of(), decision, "模型问题", "hash", false);
    }
}
