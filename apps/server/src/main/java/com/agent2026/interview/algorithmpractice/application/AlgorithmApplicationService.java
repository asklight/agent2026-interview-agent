package com.agent2026.interview.algorithmpractice.application;

import com.agent2026.interview.algorithmpractice.api.AlgorithmProblemResponse;
import com.agent2026.interview.algorithmpractice.api.AlgorithmReportResponse;
import com.agent2026.interview.algorithmpractice.api.AlgorithmSessionResponse;
import com.agent2026.interview.algorithmpractice.api.AlgorithmTurnResponse;
import com.agent2026.interview.algorithmpractice.api.SubmitAlgorithmTurnRequest;
import com.agent2026.interview.algorithmpractice.domain.AlgorithmEvaluation;
import com.agent2026.interview.algorithmpractice.domain.AlgorithmEvaluationContext;
import com.agent2026.interview.algorithmpractice.domain.AlgorithmStage;
import com.agent2026.interview.algorithmpractice.domain.AlgorithmTurnEvaluator;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmProblemEntity;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmProblemMapper;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmSessionEntity;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmTurnEntity;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class AlgorithmApplicationService {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private final AlgorithmProblemMapper problems;
    private final AlgorithmPersistenceService persistence;
    private final AlgorithmTurnEvaluator evaluator;
    private final AlgorithmReportService reports;
    private final ObjectMapper objectMapper;
    private ApplicationEventPublisher events;

    @Autowired(required = false)
    void setEvents(ApplicationEventPublisher events) {
        this.events = events;
    }

    public AlgorithmApplicationService(AlgorithmProblemMapper problems, AlgorithmPersistenceService persistence,
                                       AlgorithmTurnEvaluator evaluator, AlgorithmReportService reports,
                                       ObjectMapper objectMapper) {
        this.problems = problems;
        this.persistence = persistence;
        this.evaluator = evaluator;
        this.reports = reports;
        this.objectMapper = objectMapper;
    }

    public List<AlgorithmProblemResponse> listProblems(String difficulty, String tag) {
        LambdaQueryWrapper<AlgorithmProblemEntity> query = new LambdaQueryWrapper<AlgorithmProblemEntity>()
                .eq(AlgorithmProblemEntity::getEnabled, true);
        if (difficulty != null && !difficulty.isBlank()) {
            query.eq(AlgorithmProblemEntity::getDifficulty, difficulty.trim().toLowerCase());
        }
        if (tag != null && !tag.isBlank()) {
            query.apply("FIND_IN_SET({0}, tags) > 0", tag.trim().toLowerCase());
        }
        query.last("ORDER BY FIELD(difficulty, 'easy', 'medium', 'hard'), id");
        return problems.selectList(query).stream().map(this::problemResponse).toList();
    }

    public AlgorithmSessionResponse create(Long userId, Long problemId) {
        AlgorithmProblemEntity problem = persistence.requireProblem(problemId);
        AlgorithmSessionEntity session = persistence.create(userId, problemId,
                AlgorithmStage.CLARIFY.interviewerPrompt(null));
        return response(session, problem, persistence.turns(session.getId()));
    }

    public AlgorithmSessionResponse get(Long userId, Long sessionId) {
        AlgorithmSessionEntity session = persistence.requireOwned(sessionId, userId);
        return response(session, persistence.requireProblem(session.getProblemId()), persistence.turns(sessionId));
    }

    public AlgorithmSessionResponse submit(Long userId, Long sessionId, SubmitAlgorithmTurnRequest request) {
        String modality = normalizeModality(request.inputModality());
        AlgorithmPersistenceService.CandidateClaim claim = persistence.claimCandidate(sessionId, userId,
                request.expectedVersion(), request.clientTurnId().trim(), request.questionTurnId(),
                request.content().trim(), modality);
        if (!claim.processingOwner()) return get(userId, sessionId);

        AlgorithmSessionEntity session = claim.session();
        AlgorithmProblemEntity problem = persistence.requireProblem(session.getProblemId());
        AlgorithmStage stage = AlgorithmStage.valueOf(claim.turn().getStage());
        AlgorithmEvaluation evaluation;
        try {
            evaluation = evaluator.evaluate(new AlgorithmEvaluationContext(problem.getTitle(), problem.getStatement(),
                    readList(problem.getConstraintsJson()), readList(problem.getEvaluationRubricJson()), stage,
                    recentConversation(sessionId), claim.turn().getContent()));
        } catch (RuntimeException ex) {
            persistence.markRetryable(claim.turn().getId());
            throw new BusinessException(ErrorCode.LLM_UNAVAILABLE,
                    "面试官暂时无法完成评价，可以安全重试上一条回答", ex);
        }

        AlgorithmStage next = stage.next();
        persistence.complete(sessionId, claim.turn().getId(), evaluation, next,
                next.interviewerPrompt(next == AlgorithmStage.FOLLOW_UP ? evaluation.suggestedFollowUp() : null));
        if (next == AlgorithmStage.FINISHED) {
            reports.generateIfAbsent(sessionId);
            publishCompleted(userId, sessionId);
        }
        return get(userId, sessionId);
    }

    public AlgorithmSessionResponse retryPending(Long userId, Long sessionId) {
        AlgorithmSessionResponse session = get(userId, sessionId);
        AlgorithmTurnEntity pending = persistence.turns(sessionId).stream()
                .filter(turn -> "CANDIDATE".equals(turn.getRole()))
                .filter(turn -> "PROCESSING".equals(turn.getProcessingStatus())
                        || "RETRYABLE_FAILED".equals(turn.getProcessingStatus()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALGORITHM_STATE_CONFLICT,
                        "当前没有需要恢复的回答"));
        return submit(userId, sessionId, new SubmitAlgorithmTurnRequest(pending.getClientTurnId(),
                pending.getParentTurnId(), session.version(), pending.getContent(), pending.getInputModality()));
    }

    public AlgorithmSessionResponse finish(Long userId, Long sessionId) {
        AlgorithmSessionEntity session = persistence.finish(sessionId, userId);
        reports.generateIfAbsent(sessionId);
        publishCompleted(userId, sessionId);
        return response(session, persistence.requireProblem(session.getProblemId()), persistence.turns(sessionId));
    }

    private void publishCompleted(Long userId, Long sessionId) {
        if (events == null || userId == null) return;
        events.publishEvent(new com.agent2026.interview.shared.training.TrainingCompletedEvent(
                userId, "ALGORITHM", sessionId, 1, LocalDateTime.now()));
    }

    public AlgorithmReportResponse report(Long userId, Long sessionId) {
        AlgorithmSessionEntity session = persistence.requireOwned(sessionId, userId);
        if ("FINISHED".equals(session.getStatus())) reports.generateIfAbsent(sessionId);
        return reports.get(sessionId);
    }

    private AlgorithmSessionResponse response(AlgorithmSessionEntity session, AlgorithmProblemEntity problem,
                                              List<AlgorithmTurnEntity> turns) {
        return new AlgorithmSessionResponse(session.getId(), session.getStatus(), session.getCurrentStage(),
                session.getVersion() == null ? 0 : session.getVersion(), turnState(turns), problemResponse(problem),
                turns.stream().map(AlgorithmTurnResponse::from).toList());
    }

    private AlgorithmProblemResponse problemResponse(AlgorithmProblemEntity problem) {
        List<String> tags = problem.getTags() == null || problem.getTags().isBlank() ? List.of()
                : Arrays.stream(problem.getTags().split(",")).map(String::trim).filter(value -> !value.isBlank()).toList();
        return new AlgorithmProblemResponse(problem.getId(), problem.getProblemCode(), problem.getTitle(),
                problem.getStatement(), problem.getDifficulty(), tags, readList(problem.getConstraintsJson()));
    }

    private List<String> recentConversation(Long sessionId) {
        List<AlgorithmTurnEntity> turns = persistence.turns(sessionId);
        int start = Math.max(0, turns.size() - 8);
        return turns.subList(start, turns.size()).stream()
                .map(turn -> turn.getRole() + ": " + turn.getContent()).toList();
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("algorithm problem json invalid", ex);
        }
    }

    private String normalizeModality(String value) {
        String modality = value == null || value.isBlank() ? "TEXT" : value.trim().toUpperCase();
        if (!"TEXT".equals(modality) && !"VOICE_TRANSCRIPT".equals(modality)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "inputModality 只支持 TEXT 或 VOICE_TRANSCRIPT");
        }
        return modality;
    }

    private String turnState(List<AlgorithmTurnEntity> turns) {
        return turns.stream()
                .filter(turn -> "CANDIDATE".equals(turn.getRole()))
                .filter(turn -> !"COMPLETED".equals(turn.getProcessingStatus()))
                .reduce((first, second) -> second)
                .map(turn -> {
                    if ("RETRYABLE_FAILED".equals(turn.getProcessingStatus())) return "RETRYABLE_ERROR";
                    LocalDateTime started = turn.getProcessingStartedAt();
                    return started != null && started.isBefore(LocalDateTime.now().minusSeconds(120))
                            ? "RETRYABLE_ERROR" : "PROCESSING";
                }).orElse("IDLE");
    }
}
