package com.agent2026.interview.algorithmpractice.application;

import com.agent2026.interview.algorithmpractice.domain.AlgorithmEvaluation;
import com.agent2026.interview.algorithmpractice.domain.AlgorithmStage;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmEvaluationEntity;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmEvaluationMapper;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmProblemEntity;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmProblemMapper;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmSessionEntity;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmSessionMapper;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmTurnEntity;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmTurnMapper;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AlgorithmPersistenceService {
    private static final long PROCESSING_LEASE_SECONDS = 120;
    private final AlgorithmProblemMapper problems;
    private final AlgorithmSessionMapper sessions;
    private final AlgorithmTurnMapper turns;
    private final AlgorithmEvaluationMapper evaluations;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AlgorithmPersistenceService(AlgorithmProblemMapper problems, AlgorithmSessionMapper sessions,
                                       AlgorithmTurnMapper turns, AlgorithmEvaluationMapper evaluations,
                                       ObjectMapper objectMapper, Clock clock) {
        this.problems = problems;
        this.sessions = sessions;
        this.turns = turns;
        this.evaluations = evaluations;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public AlgorithmSessionEntity create(Long userId, Long problemId, String initialQuestion) {
        AlgorithmProblemEntity problem = requireProblem(problemId);
        AlgorithmSessionEntity session = new AlgorithmSessionEntity();
        session.setUserId(userId);
        session.setProblemId(problem.getId());
        session.setStatus("IN_PROGRESS");
        session.setCurrentStage(AlgorithmStage.CLARIFY.name());
        session.setVersion(0L);
        sessions.insert(session);
        addInterviewerTurn(session.getId(), AlgorithmStage.CLARIFY, initialQuestion, null);
        return sessions.selectById(session.getId());
    }

    @Transactional
    public CandidateClaim claimCandidate(Long sessionId, Long userId, long expectedVersion,
                                         String clientTurnId, Long questionTurnId, String content,
                                         String inputModality) {
        AlgorithmSessionEntity session = requireOwnedForUpdate(sessionId, userId);
        AlgorithmTurnEntity existing = turns.selectByClientTurnId(sessionId, clientTurnId);
        if (existing != null) {
            if ("COMPLETED".equals(existing.getProcessingStatus())) return new CandidateClaim(session, existing, false);
            LocalDateTime staleBefore = now().minusSeconds(PROCESSING_LEASE_SECONDS);
            if ("PROCESSING".equals(existing.getProcessingStatus())
                    && existing.getProcessingStartedAt() != null
                    && existing.getProcessingStartedAt().isAfter(staleBefore)) {
                throw new BusinessException(ErrorCode.ALGORITHM_TURN_PROCESSING);
            }
            existing.setProcessingStatus("RETRYABLE_FAILED");
            if (turns.claimRetry(existing.getId(), now()) != 1) {
                throw new BusinessException(ErrorCode.ALGORITHM_TURN_PROCESSING);
            }
            return new CandidateClaim(session, turns.selectById(existing.getId()), true);
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.ALGORITHM_SESSION_FINISHED);
        }
        if (session.getVersion() == null || session.getVersion() != expectedVersion) {
            throw new BusinessException(ErrorCode.ALGORITHM_STATE_CONFLICT);
        }
        AlgorithmTurnEntity question = turns.selectById(questionTurnId);
        List<AlgorithmTurnEntity> conversation = turns.selectBySession(sessionId);
        AlgorithmTurnEntity last = conversation.isEmpty() ? null : conversation.get(conversation.size() - 1);
        if (question == null || !sessionId.equals(question.getSessionId()) || !"INTERVIEWER".equals(question.getRole())
                || last == null || !question.getId().equals(last.getId())) {
            throw new BusinessException(ErrorCode.ALGORITHM_STATE_CONFLICT, "当前问题已变化，请刷新后重试");
        }

        AlgorithmTurnEntity candidate = new AlgorithmTurnEntity();
        candidate.setSessionId(sessionId);
        candidate.setSequenceNo(turns.nextSequence(sessionId));
        candidate.setRole("CANDIDATE");
        candidate.setStage(session.getCurrentStage());
        candidate.setContent(content);
        candidate.setInputModality(inputModality);
        candidate.setParentTurnId(questionTurnId);
        candidate.setClientTurnId(clientTurnId);
        candidate.setProcessingStatus("PROCESSING");
        candidate.setProcessingStartedAt(now());
        turns.insert(candidate);

        session.setVersion(session.getVersion() + 1);
        sessions.updateById(session);
        return new CandidateClaim(session, candidate, true);
    }

    @Transactional
    public void complete(Long sessionId, Long candidateTurnId, AlgorithmEvaluation evaluation,
                         AlgorithmStage nextStage, String interviewerText) {
        AlgorithmSessionEntity session = sessions.selectForUpdate(sessionId);
        AlgorithmTurnEntity candidate = turns.selectById(candidateTurnId);
        if (session == null || candidate == null || !sessionId.equals(candidate.getSessionId())) {
            throw new BusinessException(ErrorCode.ALGORITHM_STATE_CONFLICT);
        }
        if ("COMPLETED".equals(candidate.getProcessingStatus())) return;
        AlgorithmEvaluationEntity entity = new AlgorithmEvaluationEntity();
        entity.setSessionId(sessionId);
        entity.setCandidateTurnId(candidateTurnId);
        entity.setStage(candidate.getStage());
        entity.setEvaluationJson(toJson(evaluation));
        entity.setModelResponseHash(evaluation.modelResponseHash());
        entity.setDegraded(evaluation.degraded());
        evaluations.insert(entity);
        if (turns.markCompleted(candidateTurnId) != 1) {
            throw new BusinessException(ErrorCode.ALGORITHM_STATE_CONFLICT);
        }
        addInterviewerTurn(sessionId, nextStage, interviewerText, candidateTurnId);
        session.setCurrentStage(nextStage.name());
        if (nextStage == AlgorithmStage.FINISHED) {
            session.setStatus("FINISHED");
            session.setFinishedAt(now());
        }
        sessions.updateById(session);
    }

    @Transactional
    public void markRetryable(Long turnId) {
        turns.markRetryable(turnId);
    }

    @Transactional
    public AlgorithmSessionEntity finish(Long sessionId, Long userId) {
        AlgorithmSessionEntity session = requireOwnedForUpdate(sessionId, userId);
        if (!"FINISHED".equals(session.getStatus())) {
            addInterviewerTurn(sessionId, AlgorithmStage.FINISHED,
                    AlgorithmStage.FINISHED.interviewerPrompt(null), null);
            session.setCurrentStage(AlgorithmStage.FINISHED.name());
            session.setStatus("FINISHED");
            session.setFinishedAt(now());
            sessions.updateById(session);
        }
        return session;
    }

    public AlgorithmSessionEntity requireOwned(Long sessionId, Long userId) {
        AlgorithmSessionEntity session = sessions.selectById(sessionId);
        if (session == null) throw new BusinessException(ErrorCode.ALGORITHM_SESSION_NOT_FOUND);
        if (!userId.equals(session.getUserId())) throw new BusinessException(ErrorCode.ALGORITHM_SESSION_ACCESS_DENIED);
        return session;
    }

    public AlgorithmProblemEntity requireProblem(Long problemId) {
        AlgorithmProblemEntity problem = problems.selectById(problemId);
        if (problem == null || !Boolean.TRUE.equals(problem.getEnabled())) {
            throw new BusinessException(ErrorCode.ALGORITHM_PROBLEM_NOT_FOUND);
        }
        return problem;
    }

    public List<AlgorithmTurnEntity> turns(Long sessionId) { return turns.selectBySession(sessionId); }
    public List<AlgorithmEvaluationEntity> evaluations(Long sessionId) { return evaluations.selectBySession(sessionId); }

    private AlgorithmSessionEntity requireOwnedForUpdate(Long sessionId, Long userId) {
        AlgorithmSessionEntity session = sessions.selectForUpdate(sessionId);
        if (session == null) throw new BusinessException(ErrorCode.ALGORITHM_SESSION_NOT_FOUND);
        if (!userId.equals(session.getUserId())) throw new BusinessException(ErrorCode.ALGORITHM_SESSION_ACCESS_DENIED);
        return session;
    }

    private void addInterviewerTurn(Long sessionId, AlgorithmStage stage, String content, Long parentTurnId) {
        AlgorithmTurnEntity turn = new AlgorithmTurnEntity();
        turn.setSessionId(sessionId);
        turn.setSequenceNo(turns.nextSequence(sessionId));
        turn.setRole("INTERVIEWER");
        turn.setStage(stage.name());
        turn.setContent(content);
        turn.setInputModality("TEXT");
        turn.setParentTurnId(parentTurnId);
        turn.setProcessingStatus("COMPLETED");
        turns.insert(turn);
    }

    private String toJson(AlgorithmEvaluation evaluation) {
        try {
            return objectMapper.writeValueAsString(evaluation);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize algorithm evaluation", ex);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    public record CandidateClaim(AlgorithmSessionEntity session, AlgorithmTurnEntity turn, boolean processingOwner) {
    }
}
