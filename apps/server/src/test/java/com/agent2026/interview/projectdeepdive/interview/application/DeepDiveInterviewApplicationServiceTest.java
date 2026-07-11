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
import com.agent2026.interview.projectdeepdive.interview.integration.TurnEvaluationContext;
import com.agent2026.interview.projectdeepdive.interview.domain.TurnEvaluationResult;
import com.agent2026.interview.projectdeepdive.interview.knowledge.RetrievalContext;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        service.submit(8L, "token", new SubmitProjectTurnRequest("client-1", "answer", "TEXT"));
        verifyNoInteractions(evaluator);
    }

    @Test void freshProcessingTurnReturnsStableProcessingError() {
        InterviewTurn processing = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "answer", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "PROCESSING", LocalDateTime.now(), "client-1",
                LocalDateTime.now(), null, LocalDateTime.now());
        when(interviews.findByClientTurnId(8L, "client-1")).thenReturn(Optional.of(processing));
        assertThatThrownBy(() -> service.submit(8L, "token", new SubmitProjectTurnRequest("client-1", "answer", "TEXT")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INTERVIEW_TURN_PROCESSING);
        verifyNoInteractions(evaluator);
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
        when(interviews.complete(anyLong(), anyLong(), any(), any(), anyString(), any(), anyString(), anyBoolean()))
                .thenReturn(new InterviewTurn(21L, 8L, 3, "INTERVIEWER", "CLOSING", "最后总结一下", "TEXT",
                        20L, 4L, "probe-1", "OWNERSHIP", "COMPLETED", null, null, null, null, null));

        service.submit(8L, "token", new SubmitProjectTurnRequest("client-1", "篡改后的回答", "TEXT"));

        ArgumentCaptor<TurnEvaluationContext> context = ArgumentCaptor.forClass(TurnEvaluationContext.class);
        verify(evaluator).evaluate(context.capture());
        org.assertj.core.api.Assertions.assertThat(context.getValue().candidateAnswer()).isEqualTo("首次回答");
        verify(retrieval).retrieve(contains("首次回答"));
    }

    @Test void differentClientTurnCannotBypassUnresolvedProcessingTurn() {
        when(interviews.findByClientTurnId(8L, "client-2")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(eq(8L), anyLong(), eq("client-2"), anyString(), eq("TEXT"), any()))
                .thenReturn(null);
        assertThatThrownBy(() -> service.submit(8L, "token", new SubmitProjectTurnRequest("client-2", "第二个回答", "TEXT")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INTERVIEW_STATE_CONFLICT);
        verifyNoInteractions(evaluator);
    }

    @Test void answerToWrapUpQuestionFinishesAndGeneratesReport() {
        session.setConversationPhase("WRAP_UP");
        InterviewTurn candidate = new InterviewTurn(20L, 8L, 2, "CANDIDATE", "ANSWER", "我的反思", "TEXT",
                null, 4L, "probe-1", "OWNERSHIP", "PROCESSING", LocalDateTime.now(), "client-finish",
                LocalDateTime.now(), null, LocalDateTime.now());
        when(interviews.findByClientTurnId(8L, "client-finish")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(eq(8L), anyLong(), eq("client-finish"), anyString(), eq("TEXT"), any()))
                .thenReturn(candidate);
        when(retrieval.retrieve(anyString())).thenReturn(new RetrievalContext(List.of(), false));
        when(evaluator.evaluate(any())).thenReturn(new TurnEvaluationResult(Map.of("ownership", 75), List.of(),
                List.of(), List.of(), List.of(), List.of(), "WRAP_UP", "再总结", "hash", false));
        when(interviews.complete(anyLong(), anyLong(), any(), any(), eq("FINISH"), any(), contains("结束"), anyBoolean()))
                .thenReturn(new InterviewTurn(21L, 8L, 3, "INTERVIEWER", "CLOSING", "结束", "TEXT",
                        20L, 4L, "probe-1", "OWNERSHIP", "COMPLETED", null, null, null, null, null));

        service.submit(8L, "token", new SubmitProjectTurnRequest("client-finish", "我的反思", "TEXT"));

        verify(interviews).complete(anyLong(), anyLong(), any(), any(), eq("FINISH"), any(), contains("结束"), anyBoolean());
        verify(reportService).generateIfAbsent(8L);
    }

    @Test void firstCoreClaimForcesProcessEvidenceFollowUp() {
        InterviewTurn candidate = processingCandidate("forced-1", "回答");
        when(interviews.findByClientTurnId(8L, "forced-1")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(anyLong(), anyLong(), eq("forced-1"), anyString(), anyString(), any())).thenReturn(candidate);
        when(retrieval.retrieve(anyString())).thenReturn(new RetrievalContext(List.of(), false));
        when(evaluator.evaluate(any())).thenReturn(evaluation("SWITCH_DIMENSION"));
        when(interviews.complete(anyLong(), anyLong(), any(), any(), eq("FOLLOW_UP"), any(), contains("按时间顺序"), anyBoolean()))
                .thenReturn(candidate);
        service.submit(8L, "token", new SubmitProjectTurnRequest("forced-1", "回答", "TEXT"));
        verify(interviews).complete(anyLong(), anyLong(), any(), any(), eq("FOLLOW_UP"), any(), contains("按时间顺序"), anyBoolean());
    }

    @Test void secondCoreClaimFollowUpForcesTechnicalTradeoffEvidence() {
        session.setFollowUpCount(1);
        InterviewTurn candidate = processingCandidate("forced-2", "回答");
        when(interviews.findByClientTurnId(8L, "forced-2")).thenReturn(Optional.empty());
        when(interviews.registerCandidate(anyLong(), anyLong(), eq("forced-2"), anyString(), anyString(), any())).thenReturn(candidate);
        when(retrieval.retrieve(anyString())).thenReturn(new RetrievalContext(List.of(), false));
        when(evaluator.evaluate(any())).thenReturn(evaluation("SWITCH_DIMENSION"));
        when(interviews.complete(anyLong(), anyLong(), any(), any(), eq("FOLLOW_UP"), any(), contains("替代方案"), anyBoolean()))
                .thenReturn(candidate);
        service.submit(8L, "token", new SubmitProjectTurnRequest("forced-2", "回答", "TEXT"));
        verify(interviews).complete(anyLong(), anyLong(), any(), any(), eq("FOLLOW_UP"), any(), contains("替代方案"), anyBoolean());
    }

    @Test void activeProcessingTurnBlocksFinishAndReportGeneration() {
        when(interviews.finish(8L)).thenReturn(null);
        assertThatThrownBy(() -> service.finish(8L, "token"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INTERVIEW_TURN_PROCESSING);
        verifyNoInteractions(reportService);
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

    private TurnEvaluationResult evaluation(String decision) {
        return new TurnEvaluationResult(Map.of("ownership", 70), List.of(), List.of(), List.of(), List.of(),
                List.of(), decision, "模型问题", "hash", false);
    }
}
