package com.agent2026.interview.service;

import com.agent2026.interview.shared.training.CompletedTrainingReport;
import com.agent2026.interview.shared.training.CompletedTrainingReportRef;
import com.agent2026.interview.shared.training.TrainingReportCursor;
import com.agent2026.interview.shared.training.TrainingReportSourceQuery;
import com.agent2026.interview.shared.training.TrainingSourceTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class InterviewTrainingReportSourceQuery implements TrainingReportSourceQuery {
    private static final Set<String> SOURCE_TYPES = Set.of(
            TrainingSourceTypes.KNOWLEDGE, TrainingSourceTypes.PROJECT_DEEP_DIVE);
    private static final String COMPLETED_REPORTS = """
            SELECT CASE WHEN s.mode='PROJECT_DEEP_DIVE' THEN 'PROJECT_DEEP_DIVE' ELSE 'KNOWLEDGE' END source_type,
                   s.user_id, s.id source_session_id, r.id source_report_id, 1 source_report_version,
                   s.module, s.difficulty, NULL tags, s.project_profile_id, s.simulation_id,
                   r.report_json, r.strengths, r.weaknesses, r.recommendations,
                   COALESCE(r.generated_at, r.update_time, r.create_time, s.end_time, s.update_time) completed_at
            FROM interview_session s
            JOIN interview_report r ON r.session_id=s.id
            WHERE s.user_id IS NOT NULL AND s.status='FINISHED' AND r.generation_status='COMPLETED'
            """;

    private final JdbcTemplate jdbc;

    public InterviewTrainingReportSourceQuery(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Set<String> sourceTypes() {
        return SOURCE_TYPES;
    }

    @Override
    public List<CompletedTrainingReportRef> scanCompleted(String sourceType, Long userId,
                                                          TrainingReportCursor cursor, int limit) {
        requireSupported(sourceType);
        StringBuilder sql = new StringBuilder("SELECT * FROM (").append(COMPLETED_REPORTS)
                .append(") completed WHERE source_type=? AND simulation_id IS NULL AND completed_at IS NOT NULL");
        List<Object> args = new ArrayList<>();
        args.add(sourceType);
        if (userId != null) {
            sql.append(" AND user_id=?");
            args.add(userId);
        }
        if (cursor != null) {
            sql.append(" AND (completed_at<? OR (completed_at=? AND source_session_id<?))");
            args.add(cursor.completedAt());
            args.add(cursor.completedAt());
            args.add(cursor.sourceSessionId());
        }
        sql.append(" ORDER BY completed_at DESC, source_session_id DESC LIMIT ?");
        args.add(bounded(limit));
        return jdbc.query(sql.toString(), this::refRow, args.toArray());
    }

    @Override
    public Optional<CompletedTrainingReport> findReport(CompletedTrainingReportRef ref) {
        requireSupported(ref.sourceType());
        return jdbc.query("SELECT * FROM (" + COMPLETED_REPORTS + ") completed "
                        + "WHERE source_type=? AND user_id=? AND source_session_id=?",
                this::reportRow, ref.sourceType(), ref.userId(), ref.sourceSessionId()).stream().findFirst();
    }

    private CompletedTrainingReportRef refRow(ResultSet rs, int rowNum) throws SQLException {
        return new CompletedTrainingReportRef(rs.getString("source_type"), rs.getLong("user_id"),
                rs.getLong("source_session_id"), rs.getInt("source_report_version"),
                localDateTime(rs, "completed_at"));
    }

    private CompletedTrainingReport reportRow(ResultSet rs, int rowNum) throws SQLException {
        return new CompletedTrainingReport(rs.getString("source_type"), rs.getLong("user_id"),
                rs.getLong("source_session_id"), nullableLong(rs, "source_report_id"),
                rs.getInt("source_report_version"), rs.getString("module"), rs.getString("difficulty"),
                rs.getString("tags"), nullableLong(rs, "project_profile_id"), rs.getString("report_json"),
                rs.getString("strengths"), rs.getString("weaknesses"), rs.getString("recommendations"),
                localDateTime(rs, "completed_at"));
    }

    private void requireSupported(String sourceType) {
        if (!SOURCE_TYPES.contains(sourceType)) {
            throw new IllegalArgumentException("unsupported interview training source type: " + sourceType);
        }
    }

    private int bounded(int limit) {
        return Math.max(1, Math.min(500, limit));
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private java.time.LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
