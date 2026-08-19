package com.agent2026.interview.algorithmpractice.application;

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
public class AlgorithmTrainingReportSourceQuery implements TrainingReportSourceQuery {
    private static final String COMPLETED_REPORTS = """
            SELECT 'ALGORITHM' source_type, s.user_id, s.id source_session_id, r.id source_report_id,
                   1 source_report_version, NULL module, p.difficulty, p.tags, NULL project_profile_id,
                   s.simulation_id,
                   r.report_json, NULL strengths, NULL weaknesses, NULL recommendations,
                   COALESCE(r.generated_at, s.finished_at, s.update_time, s.create_time) completed_at
            FROM algorithm_session s
            JOIN algorithm_report r ON r.session_id=s.id
            JOIN algorithm_problem p ON p.id=s.problem_id
            WHERE s.user_id IS NOT NULL AND s.status='FINISHED'
            """;

    private final JdbcTemplate jdbc;

    public AlgorithmTrainingReportSourceQuery(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Set<String> sourceTypes() {
        return Set.of(TrainingSourceTypes.ALGORITHM);
    }

    @Override
    public List<CompletedTrainingReportRef> scanCompleted(String sourceType, Long userId,
                                                          TrainingReportCursor cursor, int limit) {
        requireSupported(sourceType);
        StringBuilder sql = new StringBuilder("SELECT * FROM (").append(COMPLETED_REPORTS)
                .append(") completed WHERE simulation_id IS NULL AND completed_at IS NOT NULL");
        List<Object> args = new ArrayList<>();
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
        args.add(Math.max(1, Math.min(500, limit)));
        return jdbc.query(sql.toString(), this::refRow, args.toArray());
    }

    @Override
    public Optional<CompletedTrainingReport> findReport(CompletedTrainingReportRef ref) {
        requireSupported(ref.sourceType());
        return jdbc.query("SELECT * FROM (" + COMPLETED_REPORTS + ") completed "
                        + "WHERE user_id=? AND source_session_id=?",
                this::reportRow, ref.userId(), ref.sourceSessionId()).stream().findFirst();
    }

    private CompletedTrainingReportRef refRow(ResultSet rs, int rowNum) throws SQLException {
        return new CompletedTrainingReportRef(TrainingSourceTypes.ALGORITHM, rs.getLong("user_id"),
                rs.getLong("source_session_id"), rs.getInt("source_report_version"),
                localDateTime(rs, "completed_at"));
    }

    private CompletedTrainingReport reportRow(ResultSet rs, int rowNum) throws SQLException {
        return new CompletedTrainingReport(TrainingSourceTypes.ALGORITHM, rs.getLong("user_id"),
                rs.getLong("source_session_id"), nullableLong(rs, "source_report_id"),
                rs.getInt("source_report_version"), null, rs.getString("difficulty"), rs.getString("tags"),
                null, rs.getString("report_json"), null, null, null, localDateTime(rs, "completed_at"));
    }

    private void requireSupported(String sourceType) {
        if (!TrainingSourceTypes.ALGORITHM.equals(sourceType)) {
            throw new IllegalArgumentException("unsupported algorithm training source type: " + sourceType);
        }
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
