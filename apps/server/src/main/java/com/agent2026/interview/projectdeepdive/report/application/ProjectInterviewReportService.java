package com.agent2026.interview.projectdeepdive.report.application;

import com.agent2026.interview.entity.InterviewReport;
import com.agent2026.interview.mapper.InterviewReportMapper;
import com.agent2026.interview.projectdeepdive.interview.persistence.InterviewTurnEntity;
import com.agent2026.interview.projectdeepdive.interview.persistence.InterviewTurnMapper;
import com.agent2026.interview.projectdeepdive.interview.persistence.TurnEvaluationEntity;
import com.agent2026.interview.projectdeepdive.interview.persistence.TurnEvaluationMapper;
import com.agent2026.interview.projectdeepdive.report.api.ProjectInterviewReportResponse;
import com.agent2026.interview.projectdeepdive.report.domain.ProjectInterviewReportAggregator;
import com.agent2026.interview.projectdeepdive.report.domain.ReportEvaluationFact;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProjectInterviewReportService {
    private static final TypeReference<Map<String, Integer>> SCORE_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};
    private final InterviewReportMapper reportMapper;
    private final TurnEvaluationMapper evaluationMapper;
    private final InterviewTurnMapper turnMapper;
    private final ProjectInterviewReportAggregator aggregator;
    private final ObjectMapper objectMapper;

    public ProjectInterviewReportService(InterviewReportMapper reportMapper, TurnEvaluationMapper evaluationMapper,
                                         InterviewTurnMapper turnMapper, ProjectInterviewReportAggregator aggregator,
                                         ObjectMapper objectMapper) {
        this.reportMapper = reportMapper; this.evaluationMapper = evaluationMapper; this.turnMapper = turnMapper;
        this.aggregator = aggregator; this.objectMapper = objectMapper;
    }

    public ProjectInterviewReportResponse generateIfAbsent(Long sessionId) {
        InterviewReport existing = find(sessionId);
        if (existing != null) return parse(existing);
        LocalDateTime now = LocalDateTime.now();
        ProjectInterviewReportResponse response = aggregator.aggregate(sessionId, facts(sessionId), now);
        InterviewReport report = new InterviewReport();
        report.setSessionId(sessionId); report.setMode("PROJECT_DEEP_DIVE"); report.setGenerationStatus("COMPLETED");
        report.setTotalScore(response.totalScore() == null ? BigDecimal.ZERO : BigDecimal.valueOf(response.totalScore()));
        report.setAnsweredCount(response.rounds().size()); report.setReportJson(write(response)); report.setSchemaVersion(1);
        report.setGeneratedAt(now);
        if (reportMapper.insertIgnoreProjectReport(report) == 1) return response;
        return parse(findRequired(sessionId));
    }

    public ProjectInterviewReportResponse get(Long sessionId) {
        InterviewReport report = find(sessionId);
        if (report == null || !"COMPLETED".equals(report.getGenerationStatus()) || report.getReportJson() == null)
            throw new BusinessException(ErrorCode.REPORT_NOT_READY);
        return parse(report);
    }

    private List<ReportEvaluationFact> facts(Long sessionId) {
        List<TurnEvaluationEntity> evaluations = evaluationMapper.selectList(new LambdaQueryWrapper<TurnEvaluationEntity>()
                .eq(TurnEvaluationEntity::getSessionId, sessionId).orderByAsc(TurnEvaluationEntity::getId));
        List<ReportEvaluationFact> facts = new ArrayList<>();
        for (TurnEvaluationEntity evaluation : evaluations) {
            InterviewTurnEntity candidate = turnMapper.selectById(evaluation.getCandidateTurnId());
            if (candidate == null) continue;
            facts.add(new ReportEvaluationFact(evaluation.getId(), candidate.getId(), candidate.getClaimId(),
                    evaluation.getProbeId(), candidate.getProbeDimension(), candidate.getContent(),
                    readScores(evaluation.getScoreJson()), readList(evaluation.getHitPointsJson()),
                    readList(evaluation.getMissingPointsJson()), readList(evaluation.getWeaknessesJson()),
                    readList(evaluation.getEvidenceJson()), readList(evaluation.getRiskFlagsJson())));
        }
        return List.copyOf(facts);
    }

    private InterviewReport find(Long sessionId) {
        return reportMapper.selectOne(new LambdaQueryWrapper<InterviewReport>().eq(InterviewReport::getSessionId, sessionId));
    }
    private InterviewReport findRequired(Long sessionId) {
        InterviewReport report = find(sessionId);
        if (report == null) throw new BusinessException(ErrorCode.REPORT_NOT_READY);
        return report;
    }
    private ProjectInterviewReportResponse parse(InterviewReport report) {
        try { return objectMapper.readValue(report.getReportJson(), ProjectInterviewReportResponse.class); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("project report json invalid", ex); }
    }
    private String write(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("project report serialization failed", ex); }
    }
    private Map<String, Integer> readScores(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return objectMapper.readValue(json, SCORE_TYPE); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("evaluation score json invalid", ex); }
    }
    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, LIST_TYPE); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("evaluation list json invalid", ex); }
    }
}
