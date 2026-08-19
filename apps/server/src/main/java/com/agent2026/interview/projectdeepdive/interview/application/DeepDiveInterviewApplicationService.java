package com.agent2026.interview.projectdeepdive.interview.application;

import com.agent2026.interview.entity.InterviewSession;
import com.agent2026.interview.param.CreateInterviewSessionParam;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectAnalysisStatus;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;
import com.agent2026.interview.projectdeepdive.domain.port.ProjectProfileRepository;
import com.agent2026.interview.projectdeepdive.interview.api.ProjectInterviewSessionResponse;
import com.agent2026.interview.projectdeepdive.interview.api.PublicInterviewTurnResponse;
import com.agent2026.interview.projectdeepdive.interview.api.SubmitProjectTurnRequest;
import com.agent2026.interview.projectdeepdive.interview.domain.InterviewPlan;
import com.agent2026.interview.projectdeepdive.interview.domain.InterviewTurn;
import com.agent2026.interview.projectdeepdive.interview.domain.PlannedProbe;
import com.agent2026.interview.projectdeepdive.interview.domain.ProjectDeepDivePolicy;
import com.agent2026.interview.projectdeepdive.interview.domain.ProjectInterviewPlanner;
import com.agent2026.interview.projectdeepdive.interview.domain.TurnEvaluationResult;
import com.agent2026.interview.projectdeepdive.interview.integration.TurnEvaluationContext;
import com.agent2026.interview.projectdeepdive.interview.integration.TurnEvaluator;
import com.agent2026.interview.projectdeepdive.interview.knowledge.RetrievalContext;
import com.agent2026.interview.projectdeepdive.interview.knowledge.VectorRetrievalService;
import com.agent2026.interview.projectdeepdive.interview.persistence.ProjectInterviewRepository;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.agent2026.interview.shared.security.ResourceTokenService;
import com.agent2026.interview.projectdeepdive.report.api.ProjectInterviewReportResponse;
import com.agent2026.interview.projectdeepdive.report.application.ProjectInterviewReportService;
import com.agent2026.interview.identity.application.ResourceOwnershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeepDiveInterviewApplicationService {
    private static final long PROCESSING_LEASE_SECONDS = 120;
    private final ProjectProfileRepository profileRepository;
    private final ProjectInterviewRepository interviewRepository;
    private final ProjectInterviewPlanner planner;
    private final ProjectDeepDivePolicy policy;
    private final VectorRetrievalService retrievalService;
    private final TurnEvaluator turnEvaluator;
    private final ResourceTokenService tokenService;
    private final ProjectInterviewReportService reportService;
    private final ResourceOwnershipService ownershipService;
    private ApplicationEventPublisher events;

    @Autowired(required = false)
    void setEvents(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Autowired
    public DeepDiveInterviewApplicationService(ProjectProfileRepository profileRepository,
                                               ProjectInterviewRepository interviewRepository,
                                               ProjectInterviewPlanner planner, ProjectDeepDivePolicy policy,
                                               VectorRetrievalService retrievalService, TurnEvaluator turnEvaluator,
                                               ResourceTokenService tokenService, ProjectInterviewReportService reportService,
                                               ResourceOwnershipService ownershipService) {
        this.profileRepository = profileRepository; this.interviewRepository = interviewRepository;
        this.planner = planner; this.policy = policy; this.retrievalService = retrievalService;
        this.turnEvaluator = turnEvaluator; this.tokenService = tokenService;
        this.reportService = reportService;
        this.ownershipService = ownershipService;
    }

    public DeepDiveInterviewApplicationService(ProjectProfileRepository profileRepository,
                                               ProjectInterviewRepository interviewRepository,
                                               ProjectInterviewPlanner planner, ProjectDeepDivePolicy policy,
                                               VectorRetrievalService retrievalService, TurnEvaluator turnEvaluator,
                                               ResourceTokenService tokenService, ProjectInterviewReportService reportService) {
        this(profileRepository, interviewRepository, planner, policy, retrievalService, turnEvaluator,
                tokenService, reportService, null);
    }

    public ProjectInterviewSessionResponse create(CreateInterviewSessionParam param, String token) {
        if (param.getProjectProfileId() == null || param.getProjectProfileId() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "PROJECT_DEEP_DIVE 必须提供 projectProfileId");
        }
        ProjectProfile profile = requireOwnedProfile(param.getProjectProfileId(), token);
        if (profile.analysisStatus() != ProjectAnalysisStatus.READY) throw new BusinessException(ErrorCode.PROJECT_PROFILE_NOT_READY);
        List<ProjectClaim> claims = profileRepository.findClaims(profile.id()).stream().filter(ProjectClaim::confirmed).toList();
        if (claims.isEmpty()) throw new BusinessException(ErrorCode.PROJECT_PROFILE_NOT_READY, "项目档案没有已确认声明");
        int maxFollowUps = param.getMaxFollowUpsPerClaim() == null ? 3 : param.getMaxFollowUpsPerClaim();
        if (maxFollowUps < 2) throw new BusinessException(ErrorCode.PARAM_INVALID,
                "项目深挖至少需要允许 2 次追问");
        String modality = normalizeModality(param.getInputModality());
        List<PlannedProbe> probes = planner.createPlan(claims, param.getTargetDimension());
        InterviewSession session = interviewRepository.create(profile, claims, probes, maxFollowUps, modality);
        if (ownershipService != null) ownershipService.attachInterviewToCurrentUser(session.getId());
        return response(session, interviewRepository.findPlan(session.getId()), interviewRepository.findTurns(session.getId()));
    }

    public ProjectInterviewSessionResponse getTurns(Long sessionId, String token) {
        InterviewSession session = requireOwnedSession(sessionId, token);
        return response(session, interviewRepository.findPlan(sessionId), interviewRepository.findTurns(sessionId));
    }

    public ProjectInterviewSessionResponse submit(Long sessionId, String token, SubmitProjectTurnRequest request) {
        InterviewSession session = requireOwnedSession(sessionId, token);
        String clientTurnId = request.clientTurnId().trim();
        InterviewTurn existing = interviewRepository.findByClientTurnId(sessionId, clientTurnId).orElse(null);
        if (existing != null && "COMPLETED".equals(existing.processingStatus())) {
            if ("FINISHED".equals(session.getStatus())) {
                reportService.generateIfAbsent(sessionId);
                publishCompleted(session);
            }
            return getTurns(sessionId, token);
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_FINISHED);
        }
        String answer = request.content().trim();
        String modality = normalizeModality(request.inputModality());
        InterviewPlan plan = interviewRepository.findPlan(sessionId);
        PlannedProbe probe = currentProbe(session, plan);

        InterviewTurn candidate;
        if (existing != null) {
            if ("PROCESSING".equals(existing.processingStatus()) && existing.processingStartedAt() != null
                    && existing.processingStartedAt().isAfter(LocalDateTime.now().minusSeconds(PROCESSING_LEASE_SECONDS))) {
                throw new BusinessException(ErrorCode.INTERVIEW_TURN_PROCESSING);
            }
            if (!interviewRepository.claimRetry(existing.id(), LocalDateTime.now().minusSeconds(PROCESSING_LEASE_SECONDS))) {
                InterviewTurn concurrent = interviewRepository.findByClientTurnId(sessionId, clientTurnId).orElseThrow();
                if ("COMPLETED".equals(concurrent.processingStatus())) return getTurns(sessionId, token);
                throw new BusinessException(ErrorCode.INTERVIEW_TURN_PROCESSING);
            }
            candidate = interviewRepository.findByClientTurnId(sessionId, clientTurnId).orElseThrow();
        } else {
            ProjectInterviewRepository.CandidateRegistration registration = interviewRepository.registerCandidate(
                    sessionId, session.getVersion() == null ? 0 : session.getVersion(),
                    clientTurnId, request.questionTurnId(), answer, modality, probe);
            if (registration == null) throw new BusinessException(ErrorCode.INTERVIEW_STATE_CONFLICT);
            candidate = registration.turn();
            if (!registration.processingOwner()) {
                if ("COMPLETED".equals(candidate.processingStatus())) return getTurns(sessionId, token);
                throw new BusinessException(ErrorCode.INTERVIEW_TURN_PROCESSING);
            }
        }

        String persistedAnswer = candidate.content();
        RetrievalContext retrieval;
        try { retrieval = retrievalService.retrieve(probe.objective() + "\n" + persistedAnswer); }
        catch (RuntimeException ex) { retrieval = new RetrievalContext(List.of(), true); }
        TurnEvaluationResult evaluation;
        try {
            ProjectProfile profile = profileRepository.findById(session.getProjectProfileId()).orElseThrow();
            evaluation = turnEvaluator.evaluate(new TurnEvaluationContext(profile.summary(), probe,
                    interviewRepository.findTurns(sessionId), persistedAnswer, retrieval.snippets()));
        } catch (RuntimeException ex) {
            interviewRepository.markRetryable(candidate.id(), candidate.processingStartedAt());
            throw new BusinessException(ErrorCode.LLM_UNAVAILABLE,
                    "面试官暂时无法完成评价，可以安全重试上一条回答", ex);
        }

        session = interviewRepository.findSession(sessionId).orElseThrow();
        int currentIndex = plan.plannedProbes().indexOf(probe);
        int followUps = session.getFollowUpCount() == null ? 0 : session.getFollowUpCount();
        int maxFollowUps = session.getMaxFollowUpCount() == null ? 3 : session.getMaxFollowUpCount();
        String decision;
        if ("WRAP_UP".equals(session.getConversationPhase())) decision = "FINISH";
        else if (currentIndex == 0 && followUps < 2) decision = "FOLLOW_UP";
        else decision = policy.decide(evaluation.suggestedDecision(), followUps, maxFollowUps,
                    currentIndex, plan.plannedProbes().size());
        PlannedProbe nextProbe = "FOLLOW_UP".equals(decision) ? probe
                : plan.plannedProbes().get(Math.min(currentIndex + 1, plan.plannedProbes().size() - 1));
        String nextQuestion = question(decision, evaluation.suggestedFollowUp(), nextProbe, currentIndex, followUps);
        InterviewTurn completed = interviewRepository.complete(sessionId, candidate.id(), candidate.processingStartedAt(),
                probe, evaluation, decision, nextProbe,
                nextQuestion, retrieval.degraded());
        boolean shouldGenerateReport = completed != null && "FINISH".equals(decision);
        if (completed == null) {
            InterviewTurn concurrent = interviewRepository.findByClientTurnId(sessionId, clientTurnId).orElseThrow();
            if (!"COMPLETED".equals(concurrent.processingStatus())) {
                throw new BusinessException(ErrorCode.INTERVIEW_TURN_PROCESSING);
            }
            InterviewSession persistedSession = interviewRepository.findSession(sessionId).orElseThrow();
            shouldGenerateReport = "FINISHED".equals(persistedSession.getStatus());
        }
        if (shouldGenerateReport) {
            reportService.generateIfAbsent(sessionId);
            publishCompleted(interviewRepository.findSession(sessionId).orElse(session));
        }
        return getTurns(sessionId, token);
    }

    public ProjectInterviewSessionResponse retryPending(Long sessionId, String token) {
        InterviewSession session = requireOwnedSession(sessionId, token);
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_FINISHED);
        }
        InterviewTurn pending = interviewRepository.findPendingCandidate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_STATE_CONFLICT,
                        "当前没有需要恢复的回答"));
        return submit(sessionId, token, new SubmitProjectTurnRequest(
                pending.clientTurnId(), pending.parentTurnId(), pending.content(), pending.inputModality()));
    }

    public ProjectInterviewSessionResponse finish(Long sessionId, String token) {
        requireOwnedSession(sessionId, token);
        InterviewSession session = interviewRepository.finish(sessionId);
        if (session == null) throw new BusinessException(ErrorCode.INTERVIEW_TURN_PROCESSING,
                "当前回答尚未处理完成，暂时不能结束面试");
        reportService.generateIfAbsent(sessionId);
        publishCompleted(session);
        return response(session, interviewRepository.findPlan(sessionId), interviewRepository.findTurns(sessionId));
    }

    private void publishCompleted(InterviewSession session) {
        if (events == null || session == null || session.getUserId() == null) return;
        events.publishEvent(new com.agent2026.interview.shared.training.TrainingCompletedEvent(
                session.getUserId(), "PROJECT_DEEP_DIVE", session.getId(), 1,
                session.getEndTime() == null ? LocalDateTime.now() : session.getEndTime()));
    }

    public ProjectInterviewReportResponse getReport(Long sessionId, String token) {
        InterviewSession session = requireOwnedSession(sessionId, token);
        if ("FINISHED".equals(session.getStatus())) reportService.generateIfAbsent(sessionId);
        return reportService.get(sessionId);
    }

    public boolean supports(Long sessionId) {
        return interviewRepository.findSession(sessionId).map(s -> "PROJECT_DEEP_DIVE".equals(s.getMode())).orElse(false);
    }

    public void rejectLegacyEndpoint(Long sessionId, String token) {
        requireOwnedSession(sessionId, token);
        throw new BusinessException(ErrorCode.INTERVIEW_STATE_CONFLICT, "项目深挖请使用 turns 接口");
    }

    private ProjectProfile requireOwnedProfile(Long profileId, String token) {
        ProjectProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_PROFILE_NOT_FOUND));
        requireProfileAccess(profile, token, ErrorCode.PROJECT_PROFILE_ACCESS_DENIED);
        return profile;
    }

    private InterviewSession requireOwnedSession(Long sessionId, String token) {
        InterviewSession session = interviewRepository.findSession(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));
        if (!"PROJECT_DEEP_DIVE".equals(session.getMode())) throw new BusinessException(ErrorCode.INTERVIEW_STATE_CONFLICT);
        ProjectProfile profile = profileRepository.findById(session.getProjectProfileId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));
        requireProfileAccess(profile, token, ErrorCode.INTERVIEW_SESSION_ACCESS_DENIED);
        return session;
    }

    private void requireProfileAccess(ProjectProfile profile, String token, ErrorCode deniedCode) {
        if (ownershipService != null) {
            ownershipService.requireProjectAccess(profile.id(), profile.accessTokenHash(), token, deniedCode);
            return;
        }
        if (token == null || token.isBlank() || !tokenService.matches(token, profile.accessTokenHash())) {
            throw new BusinessException(deniedCode);
        }
    }

    private PlannedProbe currentProbe(InterviewSession session, InterviewPlan plan) {
        return plan.plannedProbes().stream()
                .filter(p -> p.claimId().equals(session.getCurrentClaimId()) && p.dimension().equals(session.getCurrentProbeDimension()))
                .findFirst().orElseGet(() -> plan.plannedProbes().get(Math.min(
                        session.getCompletedQuestionCount() == null ? 0 : session.getCompletedQuestionCount(),
                        plan.plannedProbes().size() - 1)));
    }

    private String question(String decision, String suggested, PlannedProbe next, int currentProbeIndex, int followUps) {
        if ("FOLLOW_UP".equals(decision) && currentProbeIndex == 0 && followUps == 0)
            return "请按时间顺序说明你当时具体做了哪些操作，并给出一个可以核验的过程现象或结果。";
        if ("FOLLOW_UP".equals(decision) && currentProbeIndex == 0 && followUps == 1)
            return "这部分工作的关键技术依据是什么？当时还比较过什么替代方案，最终选择的代价是什么？";
        if ("FOLLOW_UP".equals(decision) && suggested != null && !suggested.isBlank()) return suggested.trim();
        if ("FINISH".equals(decision)) return "感谢你的回答，本次项目深挖到这里结束。稍后可以查看完整复盘报告。";
        if ("WRAP_UP".equals(decision)) return "最后，请总结一下这个项目中你最重要的收获，以及如果重做一次你会改变什么。";
        return switch (next.dimension()) {
            case "AUTHENTICITY" -> "请结合一个具体时间点或操作过程，说明你是怎样完成这部分工作的。";
            case "PRINCIPLE" -> "这里最关键的技术机制是什么，它在你的项目里具体解决了什么问题？";
            case "TRADEOFF" -> "当时为什么选择这个方案？还比较过哪些替代方案，代价分别是什么？";
            default -> "请继续说明你本人负责的具体动作和协作边界。";
        };
    }

    private String normalizeModality(String modality) {
        String value = modality == null || modality.isBlank() ? "TEXT" : modality.trim().toUpperCase();
        if (!"TEXT".equals(value) && !"VOICE_TRANSCRIPT".equals(value))
            throw new BusinessException(ErrorCode.PARAM_INVALID, "inputModality 只支持 TEXT 或 VOICE_TRANSCRIPT");
        return value;
    }

    private ProjectInterviewSessionResponse response(InterviewSession session, InterviewPlan plan, List<InterviewTurn> turns) {
        return new ProjectInterviewSessionResponse(session.getId(), session.getMode(), session.getStatus(),
                session.getConversationPhase(), session.getCurrentProbeDimension(),
                session.getCompletedQuestionCount() == null ? 0 : session.getCompletedQuestionCount(),
                plan.plannedProbes().size(), session.getMaxFollowUpCount() == null ? 0 : session.getMaxFollowUpCount(),
                session.getInputModality(), turnState(turns),
                turns.stream().map(PublicInterviewTurnResponse::from).toList());
    }

    private String turnState(List<InterviewTurn> turns) {
        return turns.stream()
                .filter(turn -> "CANDIDATE".equals(turn.role()))
                .filter(turn -> "PROCESSING".equals(turn.processingStatus())
                        || "RETRYABLE_FAILED".equals(turn.processingStatus()))
                .reduce((first, second) -> second)
                .map(turn -> {
                    if ("RETRYABLE_FAILED".equals(turn.processingStatus())) return "RETRYABLE_ERROR";
                    LocalDateTime startedAt = turn.processingStartedAt();
                    return startedAt == null || startedAt.isBefore(
                            LocalDateTime.now().minusSeconds(PROCESSING_LEASE_SECONDS))
                            ? "RETRYABLE_ERROR" : "PROCESSING";
                })
                .orElse("IDLE");
    }
}
