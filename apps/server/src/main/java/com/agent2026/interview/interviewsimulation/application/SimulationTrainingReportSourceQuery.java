package com.agent2026.interview.interviewsimulation.application;

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
public class SimulationTrainingReportSourceQuery implements TrainingReportSourceQuery {
    private static final String COMPLETED_REPORTS = """
            SELECT 'COMPREHENSIVE_SIMULATION' source_type, s.user_id, s.id source_session_id,
                   1 source_report_version,
                   COALESCE(r.generated_at, s.finished_at, s.update_time, s.create_time) completed_at
            FROM simulation_session s
            JOIN simulation_report r ON r.simulation_session_id=s.id
            WHERE s.user_id IS NOT NULL AND s.status='FINISHED'
            """;

    private final JdbcTemplate jdbc;

    public SimulationTrainingReportSourceQuery(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Set<String> sourceTypes() {
        return Set.of(TrainingSourceTypes.COMPREHENSIVE_SIMULATION);
    }

    @Override
    public List<CompletedTrainingReportRef> scanCompleted(String sourceType, Long userId,
                                                          TrainingReportCursor cursor, int limit) {
        requireSupported(sourceType);
        StringBuilder sql = new StringBuilder("SELECT * FROM (").append(COMPLETED_REPORTS)
                .append(") completed WHERE completed_at IS NOT NULL");
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
        return Optional.empty();
    }

    @Override
    public List<CompletedTrainingReportRef> findCompletedChildren(Long userId, Long sourceSessionId) {
        return jdbc.query("""
                SELECT CASE stage.stage_type
                         WHEN 'PROJECT' THEN 'PROJECT_DEEP_DIVE'
                         WHEN 'KNOWLEDGE' THEN 'KNOWLEDGE'
                         WHEN 'ALGORITHM' THEN 'ALGORITHM'
                       END source_type,
                       simulation.user_id, stage.business_session_id source_session_id,
                       1 source_report_version, COALESCE(stage.finished_at, simulation.finished_at) completed_at
                FROM simulation_session simulation
                JOIN simulation_report report ON report.simulation_session_id=simulation.id
                JOIN simulation_stage stage ON stage.simulation_session_id=simulation.id
                WHERE simulation.id=? AND simulation.user_id=? AND simulation.status='FINISHED'
                  AND stage.status='COMPLETED' AND stage.business_session_id IS NOT NULL
                  AND stage.stage_type IN ('PROJECT', 'KNOWLEDGE', 'ALGORITHM')
                ORDER BY stage.sequence_no
                """, this::refRow, sourceSessionId, userId);
    }

    private CompletedTrainingReportRef refRow(ResultSet rs, int rowNum) throws SQLException {
        return new CompletedTrainingReportRef(rs.getString("source_type"), rs.getLong("user_id"),
                rs.getLong("source_session_id"), rs.getInt("source_report_version"),
                localDateTime(rs, "completed_at"));
    }

    private void requireSupported(String sourceType) {
        if (!TrainingSourceTypes.COMPREHENSIVE_SIMULATION.equals(sourceType)) {
            throw new IllegalArgumentException("unsupported simulation training source type: " + sourceType);
        }
    }

    private java.time.LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
