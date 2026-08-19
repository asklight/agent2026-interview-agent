package com.agent2026.interview.trainingagent.infrastructure.persistence;

import com.agent2026.interview.trainingagent.domain.AbilityDimension;
import com.agent2026.interview.trainingagent.domain.AbilityEvidence;
import com.agent2026.interview.trainingagent.domain.AbilitySnapshot;
import com.agent2026.interview.trainingagent.domain.AbilityState;
import com.agent2026.interview.trainingagent.domain.EvidencePolarity;
import com.agent2026.interview.shared.training.CompletedTrainingReportRef;
import com.agent2026.interview.shared.training.TrainingReportCursor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class TrainingAgentMapper {
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public TrainingAgentMapper(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<CompletedTrainingReportRef> findRetryableSources(Long userId, LocalDateTime now, int limit) {
        String userClause = userId == null ? "" : " AND user_id=?";
        String sql = """
                SELECT source_type, user_id, source_session_id, source_report_version,
                       source_completed_at completed_at
                FROM training_evidence_sync_state
                WHERE (status='PENDING' OR (status='FAILED' AND next_retry_at<=?))
                """ + userClause + " ORDER BY update_time ASC LIMIT ?";
        if (userId == null) return jdbc.query(sql, this::sourceRefRow, now, limit);
        return jdbc.query(sql, this::sourceRefRow, now, userId, limit);
    }

    public boolean registerSource(CompletedTrainingReportRef ref) {
        return jdbc.update("""
                INSERT IGNORE INTO training_evidence_sync_state
                  (source_type, source_session_id, source_report_version, user_id,
                   source_completed_at, status, attempt_count)
                VALUES (?, ?, ?, ?, ?, 'PENDING', 0)
                ON DUPLICATE KEY UPDATE source_completed_at=
                  COALESCE(source_completed_at, VALUES(source_completed_at))
                """, ref.sourceType(), ref.sourceSessionId(), ref.sourceReportVersion(), ref.userId(),
                ref.completedAt()) == 1;
    }

    public boolean claimSource(CompletedTrainingReportRef ref, LocalDateTime now) {
        return jdbc.update("""
                UPDATE training_evidence_sync_state
                SET status='PROCESSING', attempt_count=attempt_count+1, last_attempt_at=?,
                    next_retry_at=NULL, last_error_code=NULL
                WHERE source_type=? AND source_session_id=? AND source_report_version=? AND user_id=?
                  AND (status='PENDING' OR (status='FAILED' AND next_retry_at<=?))
                """, now, ref.sourceType(), ref.sourceSessionId(), ref.sourceReportVersion(), ref.userId(), now) == 1;
    }

    public void markCompleted(CompletedTrainingReportRef ref) {
        updateState(ref, "COMPLETED", null, null);
    }

    public void markRejected(CompletedTrainingReportRef ref, String errorCode) {
        updateState(ref, "REJECTED", null, safeErrorCode(errorCode));
    }

    public void markFailed(CompletedTrainingReportRef ref, LocalDateTime nextRetryAt, String errorCode) {
        updateState(ref, "FAILED", nextRetryAt, safeErrorCode(errorCode));
    }

    private void updateState(CompletedTrainingReportRef ref, String status, LocalDateTime retryAt, String errorCode) {
        jdbc.update("""
                UPDATE training_evidence_sync_state
                SET status=?, next_retry_at=?, last_error_code=?
                WHERE source_type=? AND source_session_id=? AND source_report_version=? AND user_id=?
                """, status, retryAt, errorCode, ref.sourceType(), ref.sourceSessionId(),
                ref.sourceReportVersion(), ref.userId());
    }

    public int recoverStaleProcessing(LocalDateTime staleBefore, LocalDateTime retryAt) {
        return jdbc.update("""
                UPDATE training_evidence_sync_state
                SET status='FAILED', next_retry_at=?, last_error_code='PROCESSING_LEASE_EXPIRED'
                WHERE status='PROCESSING' AND last_attempt_at<?
                """, retryAt, staleBefore);
    }

    /** Current synchronization workload, including retry-delayed and actively claimed sources. */
    public int countPendingSources() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM training_evidence_sync_state
                WHERE status IN ('PENDING', 'FAILED', 'PROCESSING')
                """, Integer.class);
        return count == null ? 0 : count;
    }

    public void lockUserProjection(Long userId) {
        jdbc.queryForObject("SELECT user_id FROM training_agent_user_lock WHERE user_id=? FOR UPDATE",
                Long.class, userId);
    }

    public TrainingReportCursor findScanCursor(String sourceType) {
        return jdbc.query("""
                SELECT cursor_completed_at, cursor_session_id
                FROM training_source_scan_cursor WHERE source_type=?
                """, (rs, rowNum) -> {
            LocalDateTime completedAt = localDateTime(rs, "cursor_completed_at");
            Long sessionId = nullableLong(rs, "cursor_session_id");
            return completedAt == null || sessionId == null ? null : new TrainingReportCursor(completedAt, sessionId);
        }, sourceType).stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    public void advanceScanCursor(String sourceType, TrainingReportCursor cursor) {
        jdbc.update("""
                INSERT INTO training_source_scan_cursor
                  (source_type, cursor_completed_at, cursor_session_id, scan_cycle)
                VALUES (?, ?, ?, 0)
                ON DUPLICATE KEY UPDATE cursor_completed_at=VALUES(cursor_completed_at),
                  cursor_session_id=VALUES(cursor_session_id)
                """, sourceType, cursor.completedAt(), cursor.sourceSessionId());
    }

    public void resetScanCursor(String sourceType) {
        jdbc.update("""
                INSERT INTO training_source_scan_cursor
                  (source_type, cursor_completed_at, cursor_session_id, scan_cycle)
                VALUES (?, NULL, NULL, 1)
                ON DUPLICATE KEY UPDATE cursor_completed_at=NULL, cursor_session_id=NULL,
                  scan_cycle=scan_cycle+1
                """, sourceType);
    }

    public int insertEvidenceIgnore(AbilityEvidence evidence) {
        return jdbc.update("""
                INSERT IGNORE INTO training_ability_evidence
                  (user_id, source_type, source_session_id, source_report_id, source_report_version,
                   evidence_key, dimension_code, polarity, severity, confidence, evidence_text,
                   source_turn_id, source_evaluation_id, metadata_json, observed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, evidence.userId(), evidence.sourceType(), evidence.sourceSessionId(), evidence.sourceReportId(),
                evidence.sourceReportVersion(), evidence.evidenceKey(), evidence.dimension().code(),
                evidence.polarity().name(), evidence.severity(), evidence.confidence(), evidence.text(),
                evidence.sourceTurnId(), evidence.sourceEvaluationId(), toJson(evidence.metadata()), evidence.observedAt());
    }

    public List<AbilityEvidence> findEvidence(Long userId) {
        return jdbc.query("""
                SELECT id, user_id, source_type, source_session_id, source_report_id, source_report_version,
                       evidence_key, dimension_code, polarity, severity, confidence, evidence_text,
                       source_turn_id, source_evaluation_id, metadata_json, observed_at
                FROM training_ability_evidence WHERE user_id=? ORDER BY observed_at ASC, id ASC
                """, this::evidenceRow, userId);
    }

    public List<AbilitySnapshot> findSnapshots(Long userId) {
        return jdbc.query("""
                SELECT dimension_code, ability_state, internal_value, confidence, strength_count, gap_count,
                       risk_count, distinct_session_count, last_observed_at
                FROM user_ability_snapshot WHERE user_id=? ORDER BY dimension_code
                """, (rs, rowNum) -> new AbilitySnapshot(AbilityDimension.fromCode(rs.getString("dimension_code")),
                AbilityState.valueOf(rs.getString("ability_state")), rs.getDouble("internal_value"),
                rs.getDouble("confidence"), rs.getInt("strength_count"), rs.getInt("gap_count"),
                rs.getInt("risk_count"), rs.getInt("distinct_session_count"), localDateTime(rs, "last_observed_at")), userId);
    }

    public boolean snapshotsUsePolicy(Long userId, String policyVersion, int expectedDimensionCount) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_ability_snapshot
                WHERE user_id=? AND aggregation_policy_version=?
                """, Integer.class, userId, policyVersion);
        return count != null && count == expectedDimensionCount;
    }

    public int attemptCount(CompletedTrainingReportRef ref) {
        Integer value = jdbc.queryForObject("""
                SELECT attempt_count FROM training_evidence_sync_state
                WHERE source_type=? AND source_session_id=? AND source_report_version=? AND user_id=?
                """, Integer.class, ref.sourceType(), ref.sourceSessionId(), ref.sourceReportVersion(), ref.userId());
        return value == null ? 1 : Math.max(1, value);
    }

    public Optional<String> syncStatus(CompletedTrainingReportRef ref) {
        return jdbc.query("""
                SELECT status FROM training_evidence_sync_state
                WHERE source_type=? AND source_session_id=? AND source_report_version=? AND user_id=?
                """, (rs, rowNum) -> rs.getString("status"), ref.sourceType(), ref.sourceSessionId(),
                ref.sourceReportVersion(), ref.userId()).stream().findFirst();
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

    public Optional<StoredRecommendationRow> findRecommendation(Long userId) {
        return jdbc.query("""
                SELECT recommendation_revision, dashboard_state, primary_training_type, primary_dimension_code,
                       primary_title, primary_reason, primary_estimated_minutes, primary_action_json,
                       alternative_items_json, evidence_ids_json, recommendation_policy_version,
                       generated_at, expires_at
                FROM training_recommendation WHERE user_id=?
                """, (rs, rowNum) -> new StoredRecommendationRow(rs.getLong("recommendation_revision"),
                rs.getString("dashboard_state"), rs.getString("primary_training_type"),
                rs.getString("primary_dimension_code"), rs.getString("primary_title"), rs.getString("primary_reason"),
                rs.getInt("primary_estimated_minutes"), rs.getString("primary_action_json"),
                rs.getString("alternative_items_json"), rs.getString("evidence_ids_json"),
                rs.getString("recommendation_policy_version"), localDateTime(rs, "generated_at"),
                localDateTime(rs, "expires_at")), userId).stream().findFirst();
    }

    public long upsertRecommendation(Long userId, String state, String primaryType, String primaryDimension,
                                     String primaryTitle, String primaryReason, int estimatedMinutes,
                                     String primaryAction, String alternatives, String evidenceIds,
                                     String policyVersion, LocalDateTime generatedAt, LocalDateTime expiresAt) {
        jdbc.update("""
                INSERT INTO training_recommendation
                  (user_id, recommendation_revision, dashboard_state, primary_training_type,
                   primary_dimension_code, primary_title, primary_reason, primary_estimated_minutes,
                   primary_action_json, alternative_items_json, evidence_ids_json,
                   recommendation_policy_version, generated_at, expires_at, version)
                VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                ON DUPLICATE KEY UPDATE recommendation_revision=recommendation_revision+1,
                  dashboard_state=VALUES(dashboard_state), primary_training_type=VALUES(primary_training_type),
                  primary_dimension_code=VALUES(primary_dimension_code), primary_title=VALUES(primary_title),
                  primary_reason=VALUES(primary_reason), primary_estimated_minutes=VALUES(primary_estimated_minutes),
                  primary_action_json=VALUES(primary_action_json), alternative_items_json=VALUES(alternative_items_json),
                  evidence_ids_json=VALUES(evidence_ids_json), recommendation_policy_version=VALUES(recommendation_policy_version),
                  generated_at=VALUES(generated_at), expires_at=VALUES(expires_at), version=version+1
                """, userId, state, primaryType, primaryDimension, primaryTitle, primaryReason, estimatedMinutes,
                primaryAction, alternatives, evidenceIds, policyVersion, generatedAt, expiresAt);
        return findRecommendation(userId).map(StoredRecommendationRow::revision).orElse(1L);
    }

    private CompletedTrainingReportRef sourceRefRow(ResultSet rs, int rowNum) throws SQLException {
        LocalDateTime completedAt = localDateTime(rs, "completed_at");
        return new CompletedTrainingReportRef(rs.getString("source_type"), rs.getLong("user_id"),
                rs.getLong("source_session_id"), rs.getInt("source_report_version"), completedAt);
    }

    private AbilityEvidence evidenceRow(ResultSet rs, int rowNum) throws SQLException {
        return new AbilityEvidence(rs.getLong("id"), rs.getLong("user_id"), rs.getString("source_type"),
                rs.getLong("source_session_id"), nullableLong(rs, "source_report_id"),
                rs.getInt("source_report_version"), rs.getString("evidence_key"),
                AbilityDimension.fromCode(rs.getString("dimension_code")),
                EvidencePolarity.valueOf(rs.getString("polarity")), rs.getInt("severity"),
                rs.getDouble("confidence"), rs.getString("evidence_text"), nullableLong(rs, "source_turn_id"),
                nullableLong(rs, "source_evaluation_id"), readMetadata(rs.getString("metadata_json")),
                localDateTime(rs, "observed_at"));
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Map<String, String> readMetadata(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, STRING_MAP);
        } catch (JsonProcessingException ignored) {
            return Map.of();
        }
    }

    private String toJson(Object value) {
        if (value == null || value instanceof Map<?, ?> map && map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("training agent metadata serialization failed", ex);
        }
    }

    private String safeErrorCode(String value) {
        if (value == null || value.isBlank()) return "TRAINING_AGENT_SYNC_FAILED";
        String normalized = value.replaceAll("[^A-Za-z0-9_.-]", "_");
        return normalized.substring(0, Math.min(64, normalized.length()));
    }
}
