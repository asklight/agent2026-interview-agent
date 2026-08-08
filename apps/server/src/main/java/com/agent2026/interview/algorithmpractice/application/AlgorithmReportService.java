package com.agent2026.interview.algorithmpractice.application;

import com.agent2026.interview.algorithmpractice.api.AlgorithmReportResponse;
import com.agent2026.interview.algorithmpractice.domain.AlgorithmEvaluation;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmEvaluationEntity;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmReportEntity;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmReportMapper;
import com.agent2026.interview.algorithmpractice.infrastructure.persistence.AlgorithmTurnEntity;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AlgorithmReportService {
    private final AlgorithmReportMapper reports;
    private final AlgorithmPersistenceService persistence;
    private final AlgorithmReportAggregator aggregator;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AlgorithmReportService(AlgorithmReportMapper reports, AlgorithmPersistenceService persistence,
                                  AlgorithmReportAggregator aggregator, ObjectMapper objectMapper, Clock clock) {
        this.reports = reports;
        this.persistence = persistence;
        this.aggregator = aggregator;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public AlgorithmReportResponse generateIfAbsent(Long sessionId) {
        AlgorithmReportEntity existing = reports.selectBySession(sessionId);
        if (existing != null) return read(existing.getReportJson());
        Map<Long, AlgorithmTurnEntity> turns = persistence.turns(sessionId).stream()
                .collect(Collectors.toMap(AlgorithmTurnEntity::getId, Function.identity()));
        List<AlgorithmReportAggregator.EvaluationFact> facts = new ArrayList<>();
        for (AlgorithmEvaluationEntity entity : persistence.evaluations(sessionId)) {
            AlgorithmTurnEntity candidate = turns.get(entity.getCandidateTurnId());
            if (candidate == null) continue;
            facts.add(new AlgorithmReportAggregator.EvaluationFact(entity.getId(), candidate.getId(),
                    entity.getStage(), candidate.getContent(), readEvaluation(entity.getEvaluationJson())));
        }
        LocalDateTime generatedAt = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        AlgorithmReportResponse response = aggregator.aggregate(sessionId, facts, generatedAt);
        AlgorithmReportEntity report = new AlgorithmReportEntity();
        report.setSessionId(sessionId);
        report.setReportJson(write(response));
        report.setSchemaVersion(1);
        report.setGeneratedAt(generatedAt);
        if (reports.insertIgnore(report) == 1) return response;
        return read(reports.selectBySession(sessionId).getReportJson());
    }

    public AlgorithmReportResponse get(Long sessionId) {
        AlgorithmReportEntity report = reports.selectBySession(sessionId);
        if (report == null) throw new BusinessException(ErrorCode.REPORT_NOT_READY);
        return read(report.getReportJson());
    }

    private AlgorithmEvaluation readEvaluation(String json) {
        try {
            return objectMapper.readValue(json, AlgorithmEvaluation.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("algorithm evaluation json invalid", ex);
        }
    }

    private AlgorithmReportResponse read(String json) {
        try {
            return objectMapper.readValue(json, AlgorithmReportResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("algorithm report json invalid", ex);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("algorithm report serialization failed", ex);
        }
    }
}
