package com.agent2026.interview.projectdeepdive.report.application;

import com.agent2026.interview.entity.InterviewReport;
import com.agent2026.interview.mapper.InterviewReportMapper;
import com.agent2026.interview.projectdeepdive.interview.persistence.InterviewTurnEntity;
import com.agent2026.interview.projectdeepdive.interview.persistence.InterviewTurnMapper;
import com.agent2026.interview.projectdeepdive.interview.persistence.TurnEvaluationEntity;
import com.agent2026.interview.projectdeepdive.interview.persistence.TurnEvaluationMapper;
import com.agent2026.interview.projectdeepdive.report.api.ProjectInterviewReportResponse;
import com.agent2026.interview.projectdeepdive.report.domain.ProjectInterviewReportAggregator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectInterviewReportServiceTest {
    @Test void generatesAndPersistsStructuredReportWithNoPrivateTrace() {
        InterviewReportMapper reports = mock(InterviewReportMapper.class);
        TurnEvaluationMapper evaluations = mock(TurnEvaluationMapper.class);
        InterviewTurnMapper turns = mock(InterviewTurnMapper.class);
        TurnEvaluationEntity evaluation = new TurnEvaluationEntity();
        evaluation.setId(30L); evaluation.setSessionId(1L); evaluation.setCandidateTurnId(20L); evaluation.setProbeId("probe-1");
        evaluation.setScoreJson("{\"ownership\":80}"); evaluation.setHitPointsJson("[\"职责明确\"]");
        evaluation.setMissingPointsJson("[]"); evaluation.setWeaknessesJson("[]"); evaluation.setEvidenceJson("[]");
        evaluation.setRiskFlagsJson("[]"); evaluation.setRetrievalTraceJson("{\"secret\":true}");
        InterviewTurnEntity candidate = new InterviewTurnEntity(); candidate.setId(20L); candidate.setClaimId(10L);
        candidate.setProbeDimension("OWNERSHIP"); candidate.setContent("我的回答");
        when(evaluations.selectList(any())).thenReturn(List.of(evaluation)); when(turns.selectById(20L)).thenReturn(candidate);
        when(reports.selectOne(any())).thenReturn(null);
        when(reports.insertIgnoreProjectReport(any())).thenReturn(1);
        ProjectInterviewReportService service = new ProjectInterviewReportService(reports, evaluations, turns,
                new ProjectInterviewReportAggregator(), new ObjectMapper().registerModule(new JavaTimeModule()));

        ProjectInterviewReportResponse response = service.generateIfAbsent(1L);

        ArgumentCaptor<InterviewReport> saved = ArgumentCaptor.forClass(InterviewReport.class);
        verify(reports).insertIgnoreProjectReport(saved.capture());
        assertThat(saved.getValue().getReportJson()).doesNotContain("retrievalTrace", "secret", "modelRawResponse");
        assertThat(response.rounds().get(0).evaluationId()).isEqualTo(30L);
        assertThat(saved.getValue().getMode()).isEqualTo("PROJECT_DEEP_DIVE");
    }
}
