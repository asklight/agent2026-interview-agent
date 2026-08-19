package com.agent2026.interview.interviewsimulation.application;

import com.agent2026.interview.algorithmpractice.api.SubmitAlgorithmTurnRequest;
import com.agent2026.interview.algorithmpractice.application.AlgorithmApplicationService;
import com.agent2026.interview.algorithmpractice.application.AlgorithmReportService;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmSessionMapper;
import com.agent2026.interview.interviewsimulation.api.CreateSimulationRequest;
import com.agent2026.interview.interviewsimulation.api.SimulationOptionsResponse;
import com.agent2026.interview.interviewsimulation.api.SimulationReportResponse;
import com.agent2026.interview.interviewsimulation.api.SimulationResponse;
import com.agent2026.interview.interviewsimulation.api.SubmitSimulationAnswerRequest;
import com.agent2026.interview.interviewsimulation.domain.SimulationStageType;
import com.agent2026.interview.interviewsimulation.persistence.SimulationReportEntity;
import com.agent2026.interview.interviewsimulation.persistence.SimulationReportMapper;
import com.agent2026.interview.interviewsimulation.persistence.SimulationSessionEntity;
import com.agent2026.interview.interviewsimulation.persistence.SimulationSessionMapper;
import com.agent2026.interview.interviewsimulation.persistence.SimulationStageEntity;
import com.agent2026.interview.interviewsimulation.persistence.SimulationStageMapper;
import com.agent2026.interview.mapper.InterviewSessionMapper;
import com.agent2026.interview.param.CreateInterviewSessionParam;
import com.agent2026.interview.projectdeepdive.application.ProjectProfileApplicationService;
import com.agent2026.interview.projectdeepdive.interview.api.SubmitProjectTurnRequest;
import com.agent2026.interview.projectdeepdive.interview.application.DeepDiveInterviewApplicationService;
import com.agent2026.interview.service.InterviewReportService;
import com.agent2026.interview.service.InterviewSessionService;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SimulationApplicationService {
    private static final List<String> KNOWLEDGE_MODULES = List.of("JAVA", "MYSQL", "REDIS", "SPRING");
    private static final List<String> DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");
    private static final Set<String> KNOWLEDGE_MODULE_SET = Set.copyOf(KNOWLEDGE_MODULES);
    private static final Set<String> DIFFICULTY_SET = Set.copyOf(DIFFICULTIES);

    private final SimulationSessionMapper sessions;
    private final SimulationStageMapper stages;
    private final SimulationReportMapper reports;
    private final InterviewSessionService knowledge;
    private final SimulationKnowledgeSubmissionService knowledgeSubmissions;
    private final InterviewReportService knowledgeReports;
    private final DeepDiveInterviewApplicationService project;
    private final ProjectProfileApplicationService projectProfiles;
    private final AlgorithmApplicationService algorithm;
    private final AlgorithmReportService algorithmReports;
    private final InterviewSessionMapper interviewSessions;
    private final AlgorithmSessionMapper algorithmSessions;
    private final ObjectMapper json;
    private ApplicationEventPublisher events;

    @Autowired(required = false)
    void setEvents(ApplicationEventPublisher events) {
        this.events = events;
    }

    public SimulationApplicationService(SimulationSessionMapper sessions,
                                        SimulationStageMapper stages,
                                        SimulationReportMapper reports,
                                        InterviewSessionService knowledge,
                                        SimulationKnowledgeSubmissionService knowledgeSubmissions,
                                        InterviewReportService knowledgeReports,
                                        DeepDiveInterviewApplicationService project,
                                        ProjectProfileApplicationService projectProfiles,
                                        AlgorithmApplicationService algorithm,
                                        AlgorithmReportService algorithmReports,
                                        InterviewSessionMapper interviewSessions,
                                        AlgorithmSessionMapper algorithmSessions,
                                        ObjectMapper json) {
        this.sessions = sessions;
        this.stages = stages;
        this.reports = reports;
        this.knowledge = knowledge;
        this.knowledgeSubmissions = knowledgeSubmissions;
        this.knowledgeReports = knowledgeReports;
        this.project = project;
        this.projectProfiles = projectProfiles;
        this.algorithm = algorithm;
        this.algorithmReports = algorithmReports;
        this.interviewSessions = interviewSessions;
        this.algorithmSessions = algorithmSessions;
        this.json = json;
    }

    public SimulationOptionsResponse options(Long userId) {
        var projectOptions = projectProfiles.listReady(userId).stream()
                .map(profile -> new SimulationOptionsResponse.ProjectOption(profile.id(), profile.projectName(),
                        profile.summary(), profile.techStack()))
                .toList();
        return new SimulationOptionsResponse(projectOptions, algorithm.listProblems(null, null),
                KNOWLEDGE_MODULES, DIFFICULTIES);
    }

    @Transactional
    public SimulationResponse create(Long userId, CreateSimulationRequest request) {
        String knowledgeModule = normalizeChoice(request.knowledgeModule(), KNOWLEDGE_MODULE_SET, "八股模块");
        String difficulty = normalizeChoice(request.difficulty(), DIFFICULTY_SET, "难度");
        String clientRequestId = request.clientRequestId().trim();
        boolean projectReady = projectProfiles.listReady(userId).stream()
                .anyMatch(profile -> profile.id().equals(request.projectProfileId()));
        boolean problemEnabled = algorithm.listProblems(null, null).stream()
                .anyMatch(problem -> problem.id().equals(request.algorithmProblemId()));
        if (!projectReady || !problemEnabled) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "项目档案或算法题不可用");
        }

        SimulationSessionEntity simulation = new SimulationSessionEntity();
        simulation.setUserId(userId);
        simulation.setClientRequestId(clientRequestId);
        simulation.setProjectProfileId(request.projectProfileId());
        simulation.setAlgorithmProblemId(request.algorithmProblemId());
        simulation.setStatus("IN_PROGRESS");
        simulation.setCurrentStage(SimulationStageType.PROJECT.name());
        simulation.setVersion(0L);
        if (sessions.insertRequest(simulation) == 0) {
            SimulationSessionEntity existing = sessions.byRequest(userId, clientRequestId);
            if (existing == null) {
                throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
            }
            return response(existing, userId);
        }
        simulation = sessions.byRequest(userId, clientRequestId);
        if (simulation == null) {
            throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
        }

        long projectId = project.create(projectParams(request.projectProfileId()), null).sessionId();
        long knowledgeId = knowledge.create(knowledgeParams(knowledgeModule, difficulty), userId).getSessionId();
        long algorithmId = algorithm.create(userId, request.algorithmProblemId()).sessionId();

        interviewSessions.attachSimulation(projectId, simulation.getId());
        interviewSessions.attachSimulation(knowledgeId, simulation.getId());
        algorithmSessions.attachSimulation(algorithmId, simulation.getId());
        addStage(simulation.getId(), SimulationStageType.PROJECT, 1, projectId, "ACTIVE");
        addStage(simulation.getId(), SimulationStageType.KNOWLEDGE, 2, knowledgeId, "PENDING");
        addStage(simulation.getId(), SimulationStageType.ALGORITHM, 3, algorithmId, "PENDING");
        return response(simulation, userId);
    }

    public SimulationResponse get(Long id, Long userId) {
        return response(requireOwned(id, userId), userId);
    }

    public SimulationResponse submit(Long id, Long userId, SubmitSimulationAnswerRequest request) {
        SimulationSessionEntity simulation = requireOwned(id, userId);
        SimulationStageType stageType = activeStageType(simulation);
        SimulationStageEntity stage = currentStage(simulation);
        switch (stageType) {
            case PROJECT -> {
                requireQuestion(request);
                project.submit(stage.getBusinessSessionId(), null, new SubmitProjectTurnRequest(
                        request.clientTurnId().trim(), request.questionTurnId(), request.content().trim(),
                        request.inputModality()));
            }
            case KNOWLEDGE -> knowledgeSubmissions.submit(id, stage.getBusinessSessionId(), userId,
                    request.clientTurnId().trim(), request.content());
            case ALGORITHM -> {
                requireQuestion(request);
                if (request.expectedChildVersion() == null) {
                    throw new BusinessException(ErrorCode.PARAM_INVALID, "算法阶段缺少子会话版本");
                }
                algorithm.submit(userId, stage.getBusinessSessionId(), new SubmitAlgorithmTurnRequest(
                        request.clientTurnId().trim(), request.questionTurnId(), request.expectedChildVersion(),
                        request.content().trim(), request.inputModality()));
            }
            default -> throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
        }
        return get(id, userId);
    }

    public SimulationResponse retry(Long id, Long userId) {
        SimulationSessionEntity simulation = requireOwned(id, userId);
        SimulationStageType stageType = activeStageType(simulation);
        Long childId = currentStage(simulation).getBusinessSessionId();
        switch (stageType) {
            case PROJECT -> project.retryPending(childId, null);
            case ALGORITHM -> algorithm.retryPending(userId, childId);
            case KNOWLEDGE -> throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT,
                    "基础问答没有待恢复的回答");
            default -> throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
        }
        return get(id, userId);
    }

    @Transactional
    public SimulationResponse advance(Long id, Long userId, long expectedVersion) {
        SimulationSessionEntity simulation = requireOwned(id, userId);
        requireVersion(simulation, expectedVersion);
        SimulationStageType current = activeStageType(simulation);
        SimulationStageEntity stage = currentStage(simulation);
        if (!childFinished(current, stage.getBusinessSessionId(), userId)) {
            throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT, "当前阶段尚未结束");
        }
        if (stages.complete(stage.getId()) != 1) {
            throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
        }

        SimulationStageType next = current.next();
        if (next == SimulationStageType.REPORTING) {
            generateReport(simulation);
            next = SimulationStageType.FINISHED;
        } else {
            SimulationStageEntity nextStage = stages.byType(id, next.name());
            if (nextStage == null || stages.activate(nextStage.getId()) != 1) {
                throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
            }
        }
        if (sessions.advance(id, userId, expectedVersion, current.name(), next.name()) != 1) {
            throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
        }
        if (next == SimulationStageType.FINISHED) publishCompleted(userId, id);
        return get(id, userId);
    }

    @Transactional
    public SimulationResponse finish(Long id, Long userId, long expectedVersion) {
        SimulationSessionEntity simulation = requireOwned(id, userId);
        requireVersion(simulation, expectedVersion);
        SimulationStageType current = activeStageType(simulation);
        SimulationStageEntity stage = currentStage(simulation);
        finishChild(current, stage.getBusinessSessionId(), userId);
        if (stages.complete(stage.getId()) != 1) {
            throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
        }
        generateReport(simulation);
        if (sessions.advance(id, userId, expectedVersion, current.name(), SimulationStageType.FINISHED.name()) != 1) {
            throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
        }
        publishCompleted(userId, id);
        return get(id, userId);
    }

    private void publishCompleted(Long userId, Long simulationId) {
        if (events == null || userId == null) return;
        events.publishEvent(new com.agent2026.interview.shared.training.TrainingCompletedEvent(
                userId, "COMPREHENSIVE_SIMULATION", simulationId, 1, LocalDateTime.now()));
    }

    public SimulationReportResponse report(Long id, Long userId) {
        requireOwned(id, userId);
        SimulationReportEntity report = reports.bySimulation(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_READY);
        }
        return read(report.getReportJson());
    }

    private boolean childFinished(SimulationStageType type, Long childId, Long userId) {
        return switch (type) {
            case PROJECT -> "FINISHED".equals(project.getTurns(childId, null).status());
            case KNOWLEDGE -> "FINISHED".equals(knowledge.get(childId, userId).getStatus());
            case ALGORITHM -> "FINISHED".equals(algorithm.get(userId, childId).status());
            default -> false;
        };
    }

    private void finishChild(SimulationStageType type, Long childId, Long userId) {
        switch (type) {
            case PROJECT -> project.finish(childId, null);
            case KNOWLEDGE -> knowledge.finish(childId, userId);
            case ALGORITHM -> algorithm.finish(userId, childId);
            default -> throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
        }
    }

    private Object stageData(SimulationSessionEntity simulation, Long userId) {
        if (SimulationStageType.FINISHED.name().equals(simulation.getCurrentStage())) {
            return null;
        }
        SimulationStageEntity stage = currentStage(simulation);
        return switch (activeStageType(simulation)) {
            case PROJECT -> project.getTurns(stage.getBusinessSessionId(), null);
            case KNOWLEDGE -> knowledge.get(stage.getBusinessSessionId(), userId);
            case ALGORITHM -> algorithm.get(userId, stage.getBusinessSessionId());
            default -> null;
        };
    }

    private void generateReport(SimulationSessionEntity simulation) {
        if (reports.bySimulation(simulation.getId()) != null) {
            return;
        }
        List<SimulationReportResponse.StageReport> items = new ArrayList<>();
        for (SimulationStageEntity stage : stages.bySimulation(simulation.getId())) {
            Object stageReport = "COMPLETED".equals(stage.getStatus()) ? childReport(stage) : null;
            items.add(new SimulationReportResponse.StageReport(stage.getStageType(), stage.getStatus(), stageReport));
        }
        boolean complete = items.stream().allMatch(item -> "COMPLETED".equals(item.status()) && item.report() != null);
        List<String> recommendations = complete
                ? List.of("按项目、知识和算法三个阶段的证据逐项复盘，再安排下一场综合模拟。")
                : List.of("优先补练本次未覆盖的阶段，再进行下一场综合模拟。");
        SimulationReportResponse result = new SimulationReportResponse(1, simulation.getId(),
                complete ? "COMPLETE" : "PARTIAL", items, recommendations, LocalDateTime.now());
        SimulationReportEntity entity = new SimulationReportEntity();
        entity.setSimulationSessionId(simulation.getId());
        entity.setReportJson(write(result));
        entity.setSchemaVersion(1);
        entity.setGeneratedAt(result.generatedAt());
        reports.insertIgnore(entity);
    }

    private Object childReport(SimulationStageEntity stage) {
        return switch (SimulationStageType.valueOf(stage.getStageType())) {
            case PROJECT -> project.getReport(stage.getBusinessSessionId(), null);
            case KNOWLEDGE -> knowledgeReports.get(stage.getBusinessSessionId());
            case ALGORITHM -> algorithmReports.get(stage.getBusinessSessionId());
            default -> null;
        };
    }

    private SimulationResponse response(SimulationSessionEntity simulation, Long userId) {
        var stageResponses = stages.bySimulation(simulation.getId()).stream()
                .map(stage -> new SimulationResponse.StageResponse(stage.getStageType(), stage.getSequenceNo(),
                        stage.getStatus(), stage.getBusinessSessionId()))
                .toList();
        return new SimulationResponse(simulation.getId(), simulation.getStatus(), simulation.getCurrentStage(),
                simulation.getVersion() == null ? 0 : simulation.getVersion(), stageResponses,
                stageData(simulation, userId));
    }

    private SimulationSessionEntity requireOwned(Long id, Long userId) {
        SimulationSessionEntity value = sessions.selectById(id);
        if (value == null) {
            throw new BusinessException(ErrorCode.SIMULATION_NOT_FOUND);
        }
        if (!userId.equals(value.getUserId())) {
            throw new BusinessException(ErrorCode.SIMULATION_ACCESS_DENIED);
        }
        return value;
    }

    private SimulationStageType activeStageType(SimulationSessionEntity simulation) {
        try {
            SimulationStageType type = SimulationStageType.valueOf(simulation.getCurrentStage());
            if (type == SimulationStageType.REPORTING || type == SimulationStageType.FINISHED) {
                throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
            }
            return type;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
        }
    }

    private SimulationStageEntity currentStage(SimulationSessionEntity simulation) {
        SimulationStageEntity value = stages.byType(simulation.getId(), simulation.getCurrentStage());
        if (value == null || !"ACTIVE".equals(value.getStatus())) {
            throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
        }
        return value;
    }

    private void addStage(Long simulationId, SimulationStageType type, int sequence, Long childId, String status) {
        SimulationStageEntity stage = new SimulationStageEntity();
        stage.setSimulationSessionId(simulationId);
        stage.setStageType(type.name());
        stage.setSequenceNo(sequence);
        stage.setBusinessSessionId(childId);
        stage.setStatus(status);
        if ("ACTIVE".equals(status)) {
            stage.setStartedAt(LocalDateTime.now());
        }
        stages.insert(stage);
    }

    private CreateInterviewSessionParam projectParams(Long profileId) {
        CreateInterviewSessionParam params = new CreateInterviewSessionParam();
        params.setMode("PROJECT_DEEP_DIVE");
        params.setProjectProfileId(profileId);
        params.setDurationMinutes(10);
        params.setMaxFollowUpsPerClaim(2);
        params.setInputModality("TEXT");
        return params;
    }

    private CreateInterviewSessionParam knowledgeParams(String module, String difficulty) {
        CreateInterviewSessionParam params = new CreateInterviewSessionParam();
        params.setMode("JAVA_CORE");
        params.setModule(module);
        params.setDifficulty(difficulty);
        params.setQuestionCount(2);
        return params;
    }

    private String normalizeChoice(String value, Set<String> allowed, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, label + "不能为空");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, label + "不合法");
        }
        return normalized;
    }

    private void requireQuestion(SubmitSimulationAnswerRequest request) {
        if (request.questionTurnId() == null || request.questionTurnId() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "缺少当前问题标识");
        }
    }

    private void requireVersion(SimulationSessionEntity simulation, long expectedVersion) {
        if (simulation.getVersion() == null || simulation.getVersion().longValue() != expectedVersion) {
            throw new BusinessException(ErrorCode.SIMULATION_STATE_CONFLICT);
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("simulation report serialization failed", ex);
        }
    }

    private SimulationReportResponse read(String value) {
        try {
            return json.readValue(value, SimulationReportResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("simulation report json invalid", ex);
        }
    }
}
