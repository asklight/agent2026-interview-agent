package com.agent2026.interview.shared.training;

import com.agent2026.interview.algorithmpractice.application.AlgorithmTrainingReportSourceQuery;
import com.agent2026.interview.interviewsimulation.application.SimulationTrainingReportSourceQuery;
import com.agent2026.interview.service.InterviewTrainingReportSourceQuery;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingReportSourceQueryTest {
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 8, 19, 10, 0);

    @Test
    void interviewScanUsesKeysetAndExcludesSimulationChildren() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        InterviewTrainingReportSourceQuery query = new InterviewTrainingReportSourceQuery(jdbc);

        query.scanCompleted(TrainingSourceTypes.PROJECT_DEEP_DIVE, 7L,
                new TrainingReportCursor(COMPLETED_AT, 41L), 12);

        assertThat(jdbc.sql).contains("s.simulation_id", "simulation_id IS NULL")
                .contains("completed_at<? OR (completed_at=? AND source_session_id<?)")
                .contains("ORDER BY completed_at DESC, source_session_id DESC LIMIT ?");
        assertThat(jdbc.args).containsExactly(TrainingSourceTypes.PROJECT_DEEP_DIVE, 7L,
                COMPLETED_AT, COMPLETED_AT, 41L, 12);
    }

    @Test
    void interviewChildReportLookupDoesNotExcludeSimulationSessions() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        InterviewTrainingReportSourceQuery query = new InterviewTrainingReportSourceQuery(jdbc);
        CompletedTrainingReportRef child = new CompletedTrainingReportRef(
                TrainingSourceTypes.PROJECT_DEEP_DIVE, 7L, 42L, 1, COMPLETED_AT);

        assertThat(query.findReport(child)).isEmpty();

        assertThat(jdbc.sql).contains("source_type=? AND user_id=? AND source_session_id=?")
                .doesNotContain("simulation_id IS NULL");
        assertThat(jdbc.args).containsExactly(TrainingSourceTypes.PROJECT_DEEP_DIVE, 7L, 42L);
    }

    @Test
    void algorithmChildReportLookupDoesNotExcludeSimulationSessions() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        AlgorithmTrainingReportSourceQuery query = new AlgorithmTrainingReportSourceQuery(jdbc);
        CompletedTrainingReportRef child = new CompletedTrainingReportRef(
                TrainingSourceTypes.ALGORITHM, 7L, 52L, 1, COMPLETED_AT);

        assertThat(query.findReport(child)).isEmpty();

        assertThat(jdbc.sql).contains("user_id=? AND source_session_id=?")
                .doesNotContain("simulation_id IS NULL");
        assertThat(jdbc.args).containsExactly(7L, 52L);
    }

    @Test
    void simulationSourceExposesOnlyCompletedOwnedChildren() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        SimulationTrainingReportSourceQuery query = new SimulationTrainingReportSourceQuery(jdbc);

        assertThat(query.findCompletedChildren(7L, 61L)).isEmpty();

        assertThat(jdbc.sql).contains("simulation.id=?", "simulation.user_id=?",
                "simulation.status='FINISHED'", "stage.status='COMPLETED'",
                "stage.business_session_id IS NOT NULL")
                .contains("stage.stage_type IN ('PROJECT', 'KNOWLEDGE', 'ALGORITHM')");
        assertThat(jdbc.args).containsExactly(61L, 7L);
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private String sql;
        private Object[] args;

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.sql = sql;
            this.args = args;
            return List.of();
        }
    }
}
