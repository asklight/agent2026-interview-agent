package com.agent2026.interview.projectdeepdive.interview.persistence;

import com.agent2026.interview.entity.InterviewSession;
import com.agent2026.interview.mapper.InterviewSessionMapper;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;
import com.agent2026.interview.projectdeepdive.interview.domain.InterviewPlan;
import com.agent2026.interview.projectdeepdive.interview.domain.InterviewTurn;
import com.agent2026.interview.projectdeepdive.interview.domain.PlannedProbe;
import com.agent2026.interview.projectdeepdive.interview.domain.TurnEvaluationResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ProjectInterviewRepository {
    private static final TypeReference<List<PlannedProbe>> PROBE_LIST = new TypeReference<>() {};
    private final InterviewSessionMapper sessionMapper;
    private final InterviewPlanMapper planMapper;
    private final InterviewTurnMapper turnMapper;
    private final TurnEvaluationMapper evaluationMapper;
    private final ObjectMapper objectMapper;

    public ProjectInterviewRepository(InterviewSessionMapper sessionMapper, InterviewPlanMapper planMapper,
                                      InterviewTurnMapper turnMapper, TurnEvaluationMapper evaluationMapper,
                                      ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper; this.planMapper = planMapper; this.turnMapper = turnMapper;
        this.evaluationMapper = evaluationMapper; this.objectMapper = objectMapper;
    }

    @Transactional
    public InterviewSession create(ProjectProfile profile, List<ProjectClaim> claims, List<PlannedProbe> probes,
                                   int maxFollowUps, String inputModality) {
        LocalDateTime now = LocalDateTime.now();
        PlannedProbe first = probes.get(0);
        InterviewSession session = new InterviewSession();
        session.setMode("PROJECT_DEEP_DIVE"); session.setFeedbackTiming("AFTER_SESSION");
        session.setQuestionCount(probes.size()); session.setCompletedQuestionCount(0);
        session.setStatus("IN_PROGRESS"); session.setConversationPhase("PROJECT_OVERVIEW");
        session.setProjectProfileId(profile.id()); session.setCurrentClaimId(first.claimId());
        session.setCurrentProbeDimension(first.dimension()); session.setFollowUpCount(0);
        session.setMaxFollowUpCount(maxFollowUps); session.setInputModality(inputModality); session.setVersion(0L);
        session.setStartTime(now); sessionMapper.insert(session);

        InterviewPlanEntity plan = new InterviewPlanEntity();
        plan.setSessionId(session.getId()); plan.setProjectProfileSnapshotJson(write(java.util.Map.of(
                "profileId", profile.id(),
                "version", profile.version(),
                "projectName", profile.projectName(),
                "summary", profile.summary(),
                "techStack", profile.techStack(),
                "responsibilities", profile.responsibilities(),
                "metrics", profile.metrics(),
                "architecture", profile.architecture(),
                "claims", claims
        )));
        plan.setPlannedProbesJson(write(probes)); plan.setTemplateVersion(1); plan.setStatus("ACTIVE"); plan.setCreateTime(now);
        planMapper.insert(plan);

        InterviewTurnEntity opening = new InterviewTurnEntity();
        opening.setSessionId(session.getId()); opening.setSequenceNo(1); opening.setRole("INTERVIEWER");
        opening.setTurnType("OPENING");
        opening.setContent("请先用一到两分钟介绍这个项目，并重点说明你本人负责的部分。");
        opening.setInputModality("TEXT"); opening.setClaimId(first.claimId()); opening.setProbeId(first.probeId());
        opening.setProbeDimension(first.dimension()); opening.setProcessingStatus("COMPLETED");
        opening.setStartedAt(now); opening.setEndedAt(now); opening.setCreateTime(now); turnMapper.insert(opening);
        return session;
    }

    public Optional<InterviewSession> findSession(Long id) { return Optional.ofNullable(sessionMapper.selectById(id)); }

    public InterviewPlan findPlan(Long sessionId) {
        InterviewPlanEntity entity = planMapper.selectOne(new LambdaQueryWrapper<InterviewPlanEntity>()
                .eq(InterviewPlanEntity::getSessionId, sessionId));
        if (entity == null) throw new IllegalStateException("interview plan not found");
        return new InterviewPlan(entity.getId(), entity.getSessionId(), entity.getProjectProfileSnapshotJson(),
                readProbes(entity.getPlannedProbesJson()), entity.getTemplateVersion(), entity.getStatus());
    }

    public List<InterviewTurn> findTurns(Long sessionId) {
        return turnMapper.selectList(new LambdaQueryWrapper<InterviewTurnEntity>()
                        .eq(InterviewTurnEntity::getSessionId, sessionId).orderByAsc(InterviewTurnEntity::getSequenceNo))
                .stream().map(this::toDomain).toList();
    }

    public Optional<InterviewTurn> findByClientTurnId(Long sessionId, String clientTurnId) {
        InterviewTurnEntity entity = turnMapper.selectOne(new LambdaQueryWrapper<InterviewTurnEntity>()
                .eq(InterviewTurnEntity::getSessionId, sessionId)
                .eq(InterviewTurnEntity::getClientTurnId, clientTurnId));
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Transactional
    public InterviewTurn registerCandidate(Long sessionId, long expectedVersion, String clientTurnId,
                                           String content, String inputModality, PlannedProbe probe) {
        InterviewSession locked = sessionMapper.selectForUpdate(sessionId);
        if (locked == null || !"IN_PROGRESS".equals(locked.getStatus())) return null;
        Optional<InterviewTurn> duplicate = findByClientTurnId(sessionId, clientTurnId);
        if (duplicate.isPresent()) return duplicate.get();
        if (turnMapper.countProcessingCandidates(sessionId) > 0) return null;
        if (locked.getVersion() == null || locked.getVersion() != expectedVersion
                || sessionMapper.reserveTurn(sessionId, expectedVersion) != 1) return null;
        LocalDateTime now = LocalDateTime.now();
        InterviewTurnEntity entity = new InterviewTurnEntity();
        entity.setSessionId(sessionId); entity.setSequenceNo(turnMapper.maxSequence(sessionId) + 1);
        entity.setRole("CANDIDATE"); entity.setTurnType("ANSWER"); entity.setContent(content);
        entity.setInputModality(inputModality); entity.setClaimId(probe.claimId()); entity.setProbeId(probe.probeId());
        entity.setProbeDimension(probe.dimension()); entity.setProcessingStatus("PROCESSING");
        entity.setProcessingStartedAt(now); entity.setClientTurnId(clientTurnId); entity.setStartedAt(now); entity.setCreateTime(now);
        try { turnMapper.insert(entity); } catch (DuplicateKeyException ex) { return findByClientTurnId(sessionId, clientTurnId).orElseThrow(); }
        return toDomain(entity);
    }

    @Transactional
    public boolean claimRetry(Long turnId, LocalDateTime staleBefore) {
        return turnMapper.claimRetry(turnId, LocalDateTime.now(), staleBefore) == 1;
    }

    @Transactional public void markRetryable(Long turnId) { turnMapper.markRetryable(turnId); }

    @Transactional
    public InterviewTurn complete(Long sessionId, Long candidateTurnId, PlannedProbe currentProbe,
                                  TurnEvaluationResult result, String decision, PlannedProbe nextProbe,
                                  String interviewerContent, boolean retrievalDegraded) {
        InterviewSession session = sessionMapper.selectForUpdate(sessionId);
        InterviewTurnEntity candidate = turnMapper.selectById(candidateTurnId);
        if (session == null || !"IN_PROGRESS".equals(session.getStatus()) || candidate == null
                || !"PROCESSING".equals(candidate.getProcessingStatus())) return null;
        LocalDateTime now = LocalDateTime.now();
        TurnEvaluationEntity evaluation = new TurnEvaluationEntity();
        evaluation.setSessionId(sessionId); evaluation.setCandidateTurnId(candidateTurnId); evaluation.setProbeId(currentProbe.probeId());
        evaluation.setScoreJson(write(result.scores())); evaluation.setHitPointsJson(write(result.hitPoints()));
        evaluation.setMissingPointsJson(write(result.missingPoints())); evaluation.setWeaknessesJson(write(result.weaknesses()));
        evaluation.setEvidenceJson(write(result.evidence())); evaluation.setRiskFlagsJson(write(result.riskFlags()));
        evaluation.setDecision(decision); evaluation.setSuggestedFollowUp(result.suggestedFollowUp());
        evaluation.setRetrievalTraceJson(write(java.util.Map.of("provider", "NO_OP", "degraded", retrievalDegraded)));
        evaluation.setModelResponseHash(result.modelResponseHash()); evaluation.setModelSchemaVersion(1);
        evaluation.setDegraded(result.degraded()); evaluation.setCreateTime(now); evaluationMapper.insert(evaluation);

        InterviewTurnEntity interviewer = new InterviewTurnEntity();
        interviewer.setSessionId(sessionId); interviewer.setSequenceNo(turnMapper.maxSequence(sessionId) + 1);
        interviewer.setRole("INTERVIEWER"); interviewer.setTurnType("FOLLOW_UP".equals(decision) ? "FOLLOW_UP" :
                (("WRAP_UP".equals(decision) || "FINISH".equals(decision)) ? "CLOSING" : "TRANSITION"));
        interviewer.setContent(interviewerContent); interviewer.setInputModality("TEXT"); interviewer.setParentTurnId(candidateTurnId);
        interviewer.setClaimId(nextProbe.claimId()); interviewer.setProbeId(nextProbe.probeId()); interviewer.setProbeDimension(nextProbe.dimension());
        interviewer.setProcessingStatus("COMPLETED"); interviewer.setStartedAt(now); interviewer.setEndedAt(now); interviewer.setCreateTime(now);
        turnMapper.insert(interviewer);
        if (turnMapper.markCompleted(candidateTurnId, now) != 1) throw new IllegalStateException("candidate turn changed concurrently");

        if ("FOLLOW_UP".equals(decision)) session.setFollowUpCount((session.getFollowUpCount() == null ? 0 : session.getFollowUpCount()) + 1);
        else {
            session.setFollowUpCount(0);
            int completed = (session.getCompletedQuestionCount() == null ? 0 : session.getCompletedQuestionCount()) + 1;
            session.setCompletedQuestionCount(Math.min(completed,
                    session.getQuestionCount() == null ? completed : session.getQuestionCount()));
        }
        session.setCurrentClaimId(nextProbe.claimId()); session.setCurrentProbeDimension(nextProbe.dimension());
        session.setConversationPhase(phase(nextProbe.dimension(), decision));
        if ("FINISH".equals(decision)) {
            session.setStatus("FINISHED"); session.setEndTime(now);
            InterviewPlanEntity plan = planMapper.selectOne(new LambdaQueryWrapper<InterviewPlanEntity>()
                    .eq(InterviewPlanEntity::getSessionId, sessionId));
            if (plan != null) { plan.setStatus("COMPLETED"); planMapper.updateById(plan); }
        }
        session.setVersion((session.getVersion() == null ? 0 : session.getVersion()) + 1); sessionMapper.updateById(session);
        return toDomain(interviewer);
    }

    @Transactional
    public InterviewSession finish(Long sessionId) {
        InterviewSession session = sessionMapper.selectForUpdate(sessionId);
        if (session != null && turnMapper.countProcessingCandidates(sessionId) > 0) return null;
        if (session != null && !"FINISHED".equals(session.getStatus())) {
            session.setStatus("FINISHED"); session.setConversationPhase("WRAP_UP"); session.setEndTime(LocalDateTime.now());
            session.setVersion((session.getVersion() == null ? 0 : session.getVersion()) + 1); sessionMapper.updateById(session);
            InterviewPlanEntity plan = planMapper.selectOne(new LambdaQueryWrapper<InterviewPlanEntity>().eq(InterviewPlanEntity::getSessionId, sessionId));
            if (plan != null) { plan.setStatus("COMPLETED"); planMapper.updateById(plan); }
        }
        return session;
    }

    private String phase(String dimension, String decision) {
        if ("WRAP_UP".equals(decision) || "FINISH".equals(decision)) return "WRAP_UP";
        if ("OWNERSHIP".equals(dimension) || "AUTHENTICITY".equals(dimension)) return "CLAIM_DEEP_DIVE";
        if ("PRINCIPLE".equals(dimension)) return "TECHNICAL_PROBE";
        return "TRADEOFF_OR_INCIDENT";
    }

    private InterviewTurn toDomain(InterviewTurnEntity e) {
        return new InterviewTurn(e.getId(), e.getSessionId(), e.getSequenceNo(), e.getRole(), e.getTurnType(), e.getContent(),
                e.getInputModality(), e.getParentTurnId(), e.getClaimId(), e.getProbeId(), e.getProbeDimension(),
                e.getProcessingStatus(), e.getProcessingStartedAt(), e.getClientTurnId(), e.getStartedAt(), e.getEndedAt(), e.getCreateTime());
    }
    private String write(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException ex) { throw new IllegalStateException("interview json serialization failed", ex); } }
    private List<PlannedProbe> readProbes(String json) { try { return objectMapper.readValue(json, PROBE_LIST); } catch (JsonProcessingException ex) { throw new IllegalStateException("interview plan json invalid", ex); } }
}
