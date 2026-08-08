package com.agent2026.interview.interviewsimulation.application;

import com.agent2026.interview.algorithmpractice.api.AlgorithmSessionResponse;
import com.agent2026.interview.algorithmpractice.api.AlgorithmProblemResponse;
import com.agent2026.interview.algorithmpractice.application.AlgorithmApplicationService;
import com.agent2026.interview.algorithmpractice.application.AlgorithmReportService;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmSessionMapper;
import com.agent2026.interview.interviewsimulation.api.CreateSimulationRequest;
import com.agent2026.interview.interviewsimulation.persistence.SimulationReportMapper;
import com.agent2026.interview.interviewsimulation.persistence.SimulationSessionEntity;
import com.agent2026.interview.interviewsimulation.persistence.SimulationSessionMapper;
import com.agent2026.interview.interviewsimulation.persistence.SimulationStageEntity;
import com.agent2026.interview.interviewsimulation.persistence.SimulationStageMapper;
import com.agent2026.interview.mapper.InterviewSessionMapper;
import com.agent2026.interview.projectdeepdive.application.ProjectProfileApplicationService;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectAnalysisStatus;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;
import com.agent2026.interview.projectdeepdive.interview.api.ProjectInterviewSessionResponse;
import com.agent2026.interview.projectdeepdive.interview.application.DeepDiveInterviewApplicationService;
import com.agent2026.interview.service.InterviewReportService;
import com.agent2026.interview.service.InterviewSessionService;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.vo.InterviewSessionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimulationApplicationServiceTest {
    private final SimulationSessionMapper sessions = mock(SimulationSessionMapper.class);
    private final SimulationStageMapper stages = mock(SimulationStageMapper.class);
    private final SimulationReportMapper reports = mock(SimulationReportMapper.class);
    private final InterviewSessionService knowledge = mock(InterviewSessionService.class);
    private final SimulationKnowledgeSubmissionService knowledgeSubmissions = mock(SimulationKnowledgeSubmissionService.class);
    private final InterviewReportService knowledgeReports = mock(InterviewReportService.class);
    private final DeepDiveInterviewApplicationService project = mock(DeepDiveInterviewApplicationService.class);
    private final ProjectProfileApplicationService projectProfiles = mock(ProjectProfileApplicationService.class);
    private final AlgorithmApplicationService algorithm = mock(AlgorithmApplicationService.class);
    private final AlgorithmReportService algorithmReports = mock(AlgorithmReportService.class);
    private final InterviewSessionMapper interviewSessions = mock(InterviewSessionMapper.class);
    private final AlgorithmSessionMapper algorithmSessions = mock(AlgorithmSessionMapper.class);
    private final SimulationApplicationService service = new SimulationApplicationService(sessions, stages, reports,
            knowledge, knowledgeSubmissions, knowledgeReports, project, projectProfiles, algorithm, algorithmReports, interviewSessions,
            algorithmSessions, new ObjectMapper().findAndRegisterModules());

    @Test
    void createsEachChildSessionExactlyOnce() {
        SimulationSessionEntity persisted = simulation(50L, 7L, 0L, "PROJECT");
        persisted.setClientRequestId("request-1");
        when(sessions.insertRequest(any(SimulationSessionEntity.class))).thenReturn(1);
        when(sessions.byRequest(7L, "request-1")).thenReturn(persisted);
        when(projectProfiles.listReady(7L)).thenReturn(List.of(new ProjectProfile(8L, "hash", "description",
                "RegPilot", "summary", List.of("Java"), List.of(), List.of(), List.of(), List.of(),
                ProjectAnalysisStatus.READY, 1, null, null)));
        when(algorithm.listProblems(null, null)).thenReturn(List.of(new AlgorithmProblemResponse(
                9L, "lru", "LRU", "statement", "medium", List.of(), List.of())));
        when(project.create(any(), eq(null))).thenReturn(projectSession(11L, "IN_PROGRESS"));
        InterviewSessionVO knowledgeSession = new InterviewSessionVO();
        knowledgeSession.setSessionId(12L);
        when(knowledge.create(any(), eq(7L))).thenReturn(knowledgeSession);
        when(algorithm.create(7L, 9L)).thenReturn(new AlgorithmSessionResponse(
                13L, "IN_PROGRESS", "CLARIFY", 0, "IDLE", null, List.of()));
        List<SimulationStageEntity> insertedStages = new ArrayList<>();
        doAnswer(invocation -> {
            SimulationStageEntity stage = invocation.getArgument(0);
            stage.setId((long) insertedStages.size() + 1);
            insertedStages.add(stage);
            return 1;
        }).when(stages).insert(any(SimulationStageEntity.class));
        when(stages.bySimulation(50L)).thenAnswer(invocation -> insertedStages);
        when(stages.byType(50L, "PROJECT")).thenAnswer(invocation -> insertedStages.get(0));
        when(project.getTurns(11L, null)).thenReturn(projectSession(11L, "IN_PROGRESS"));

        var result = service.create(7L, new CreateSimulationRequest("request-1", 8L, 9L, "JAVA", "MEDIUM"));

        assertThat(result.simulationId()).isEqualTo(50L);
        assertThat(result.stages()).extracting(stage -> stage.businessSessionId())
                .containsExactly(11L, 12L, 13L);
        verify(project).create(any(), eq(null));
        verify(knowledge).create(any(), eq(7L));
        verify(algorithm).create(7L, 9L);
        verify(interviewSessions).attachSimulation(11L, 50L);
        verify(interviewSessions).attachSimulation(12L, 50L);
        verify(algorithmSessions).attachSimulation(13L, 50L);
    }

    @Test
    void repeatedCreateRequestReturnsTheExistingSimulation() {
        SimulationSessionEntity existing = simulation(50L, 7L, 0L, "PROJECT");
        existing.setClientRequestId("request-1");
        SimulationStageEntity active = new SimulationStageEntity();
        active.setId(1L); active.setSimulationSessionId(50L); active.setStageType("PROJECT");
        active.setSequenceNo(1); active.setBusinessSessionId(11L); active.setStatus("ACTIVE");
        when(projectProfiles.listReady(7L)).thenReturn(List.of(new ProjectProfile(8L, "hash", "description",
                "RegPilot", "summary", List.of("Java"), List.of(), List.of(), List.of(), List.of(),
                ProjectAnalysisStatus.READY, 1, null, null)));
        when(algorithm.listProblems(null, null)).thenReturn(List.of(new AlgorithmProblemResponse(
                9L, "lru", "LRU", "statement", "medium", List.of(), List.of())));
        when(sessions.insertRequest(any(SimulationSessionEntity.class))).thenReturn(0);
        when(sessions.byRequest(7L, "request-1")).thenReturn(existing);
        when(stages.bySimulation(50L)).thenReturn(List.of(active));
        when(stages.byType(50L, "PROJECT")).thenReturn(active);
        when(project.getTurns(11L, null)).thenReturn(projectSession(11L, "IN_PROGRESS"));

        var result = service.create(7L, new CreateSimulationRequest("request-1", 8L, 9L, "JAVA", "MEDIUM"));

        assertThat(result.simulationId()).isEqualTo(50L);
        verify(project, never()).create(any(), any());
        verify(knowledge, never()).create(any(), any());
        verify(algorithm, never()).create(any(), any());
    }

    @Test
    void rejectsAnotherUsersSimulation() {
        SimulationSessionEntity simulation = simulation(50L, 8L, 0L, "PROJECT");
        when(sessions.selectById(50L)).thenReturn(simulation);

        assertThatThrownBy(() -> service.get(50L, 7L)).isInstanceOf(BusinessException.class);
        verify(stages, never()).bySimulation(any());
    }

    @Test
    void versionConflictCannotAdvanceAStage() {
        SimulationSessionEntity simulation = simulation(50L, 7L, 2L, "PROJECT");
        when(sessions.selectById(50L)).thenReturn(simulation);

        assertThatThrownBy(() -> service.advance(50L, 7L, 1L)).isInstanceOf(BusinessException.class);
        verify(stages, never()).complete(any());
        verify(sessions, never()).advance(any(), any(), any(Long.class), any(), any());
    }

    private SimulationSessionEntity simulation(Long id, Long userId, Long version, String stage) {
        SimulationSessionEntity value = new SimulationSessionEntity();
        value.setId(id);
        value.setUserId(userId);
        value.setVersion(version);
        value.setStatus("IN_PROGRESS");
        value.setCurrentStage(stage);
        return value;
    }

    private ProjectInterviewSessionResponse projectSession(Long id, String status) {
        return new ProjectInterviewSessionResponse(id, "PROJECT_DEEP_DIVE", status, "OPENING", null,
                0, 3, 2, "TEXT", "IDLE", List.of());
    }
}
