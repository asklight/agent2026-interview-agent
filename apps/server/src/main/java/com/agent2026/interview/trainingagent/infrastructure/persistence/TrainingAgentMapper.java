package com.agent2026.interview.trainingagent.infrastructure.persistence;

import com.agent2026.interview.trainingagent.domain.AbilityDimension;
import com.agent2026.interview.trainingagent.domain.AbilityEvidence;
import com.agent2026.interview.trainingagent.domain.AbilitySnapshot;
import com.agent2026.interview.trainingagent.domain.AbilityState;
import com.agent2026.interview.trainingagent.domain.EvidencePolarity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class TrainingAgentMapper {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public TrainingAgentMapper(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<TrainingAgentSourceRow> findCompletedReports(Long userId, int limit) {
        String interviewSql = """
                SELECT CASE WHEN s.mode='PROJECT_DEEP_DIVE' THEN 'PROJECT_DEEP_DIVE' ELSE 'KNOWLEDGE' END AS source_type,
                       s.user_id, s.id AS source_session_id, r.id AS source_report_id,
                       COALESCE(r.schema_version, 1) AS source_report_version, s.module,
                       r.report_json, r.strengths, r.weaknesses, r.recommendations,
                       COALESCE(r.generated_at, r.update_time, r.create_time, s.end_time, s.update_time) AS observed_at
                FROM interview_session s
                JOIN interview_report r ON r.session_id=s.id
                WHERE s.user_id=? AND s.status='FINISHED'
                  AND r.generation_status='COMPLETED' AND s.simulation_id IS NULL
                ORDER BY observed_at DESC LIMIT ?
                """;
        String algorithmSql = """
                SELECT 'ALGORITHM' AS source_type, s.user_id, s.id AS source_session_id, r.id AS source_report_id,
                       COALESCE(r.schema_version, 1) AS source_report_version, NULL AS module,
                       r.report_json, NULL AS strengths, NULL AS weaknesses, NULL AS recommendations,
                       COALESCE(r.generated_at, s.finished_at, s.update_time, s.create_time) AS observed_at
                FROM algorithm_session s
                JOIN algorithm_report r ON r.session_id=s.id
                WHERE s.user_id=? AND s.status='FINISHED' AND s.simulation_id IS NULL
                ORDER BY observed_at DESC LIMIT ?
                """;
        List<TrainingAgentSourceRow> rows = jdbc.query(interviewSql, this::sourceRow, userId, limit);
        rows = new java.util.ArrayList<>(rows);
        rows.addAll(jdbc.query(algorithmSql, this::sourceRow, userId, limit));
        rows.sort(java.util.Comparator.comparing(TrainingAgentSourceRow::observedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        return rows.size() > limit ? rows.subList(0, limit) : List.copyOf(rows);
    }

    private TrainingAgentSourceRow sourceRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp("observed_at");
        return new TrainingAgentSourceRow(rs.getString("source_type"), rs.getLong("user_id"),
                rs.getLong("source_session_id"), rs.getLong("source_report_id"),
                rs.getInt("source_report_version"), rs.getString("module"), rs.getString("report_json"),
                rs.getString("strengths"), rs.getString("weaknesses"), rs.getString("recommendations"),
                timestamp == null ? null : timestamp.toLocalDateTime());
    }

    public void insertEvidenceIgnore(AbilityEvidence evidence) {
        jdbc.update("""
                INSERT IGNORE INTO training_ability_evidence
                  (user_id, source_type, source_session_id, source_report_id, source_report_version,
                   evidence_key, dimension_code, polarity, severity, confidence, evidence_text,
                   source_turn_id, source_evaluation_id, metadata_json, observed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, evidence.userId(), evidence.sourceType(), evidence.sourceSessionId(), evidence.sourceReportId(),
                evidence.sourceReportVersion(), evidence.evidenceKey(), evidence.dimension().code(),
                evidence.polarity().name(), evidence.severity(), evidence.confidence(), evidence.text(),
                evidence.sourceTurnId(), evidence.sourceEvaluationId(), toJson(evidence.metadata()),
                evidence.observedAt());
    }

    public List<AbilityEvidence> findEvidence(Long userId) {
        return jdbc.query("""
                SELECT id, user_id, source_type, source_session_id, source_report_id, source_report_version,
                       evidence_key, dimension_code, polarity, severity, confidence, evidence_text,
                       source_turn_id, source_evaluation_id, metadata_json, observed_at
                FROM training_ability_evidence WHERE user_id=? ORDER BY observed_at ASC, id ASC
                """, (rs, rowNum) -> {
            Timestamp timestamp = rs.getTimestamp("observed_at");
            return new AbilityEvidence(rs.getLong("id"), rs.getLong("user_id"), rs.getString("source_type"),
                    rs.getLong("source_session_id"), nullableLong(rs, "source_report_id"),
                    rs.getInt("source_report_version"), rs.getString("evidence_key"),
                    AbilityDimension.fromCode(rs.getString("dimension_code")),
                    EvidencePolarity.valueOf(rs.getString("polarity")), rs.getInt("severity"),
                    rs.getDouble("confidence"), rs.getString("evidence_text"),
                    nullableLong(rs, "source_turn_id"), nullableLong(rs, "source_evaluation_id"),
                    readMetadata(rs.getString("metadata_json")),
                    timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime());
        }, userId);
    }

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Map<String, String> readMetadata(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    public void upsertSnapshots(Long userId, List<AbilitySnapshot> snapshots, String policyVersion) {
        for (AbilitySnapshot snapshot : snapshots) {
            jdbc.update("""
                    INSERT INTO user_ability_snapshot
                      (user_id, dimension_code, ability_state, internal_value, confidence, strength_count,
                       gap_count, risk_count, distinct_session_count, last_observed_at,
                       aggregation_policy_version, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                    ON DUPLICATE KEY UPDATE ability_state=VALUES(ability_state), internal_value=VALUES(internal_value),
                      confidence=VALUES(confidence), strength_count=VALUES(strength_count), gap_count=VALUES(gap_count),
                      risk_count=VALUES(risk_count), distinct_session_count=VALUES(distinct_session_count),
                      last_observed_at=VALUES(last_observed_at), aggregation_policy_version=VALUES(aggregation_policy_version),
                      version=version+1
                    """, userId, snapshot.dimension().code(), snapshot.state().name(), snapshot.internalValue(),
                    snapshot.confidence(), snapshot.strengthCount(), snapshot.gapCount(), snapshot.riskCount(),
                    snapshot.distinctSessionCount(), snapshot.lastObservedAt(), policyVersion);
        }
    }

    public void upsertRecommendation(Long userId, String state, String primaryType, String primaryDimension,
                                     String primaryTitle, String primaryReason, String primaryAction,
                                     String alternatives, String evidenceIds, String policyVersion,
                                     LocalDateTime generatedAt, LocalDateTime expiresAt) {
        jdbc.update("""
                INSERT INTO training_recommendation
                  (user_id, recommendation_revision, dashboard_state, primary_training_type,
                   primary_dimension_code, primary_title, primary_reason, primary_action_json,
                   alternative_items_json, evidence_ids_json, recommendation_policy_version,
                   generated_at, expires_at, version)
                VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                ON DUPLICATE KEY UPDATE recommendation_revision=recommendation_revision+1,
                  dashboard_state=VALUES(dashboard_state), primary_training_type=VALUES(primary_training_type),
                  primary_dimension_code=VALUES(primary_dimension_code), primary_title=VALUES(primary_title),
                  primary_reason=VALUES(primary_reason), primary_action_json=VALUES(primary_action_json),
                  alternative_items_json=VALUES(alternative_items_json), evidence_ids_json=VALUES(evidence_ids_json),
                  recommendation_policy_version=VALUES(recommendation_policy_version), generated_at=VALUES(generated_at),
                  expires_at=VALUES(expires_at), version=version+1
                """, userId, state, primaryType, primaryDimension, primaryTitle, primaryReason, primaryAction,
                alternatives, evidenceIds, policyVersion, generatedAt, expiresAt);
    }

    private String toJson(Object value) {
        if (value == null || value instanceof Map<?, ?> map && map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("training agent metadata serialization failed", ex);
        }
    }
}
