package com.agent2026.interview.trainingagent.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.agent2026.interview.shared.training.CompletedTrainingReportRef;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingAgentMapperQueryTest {

    @Test
    void scanCursorUsesOnlyTrainingAgentOwnedTables() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        TrainingAgentMapper mapper = new TrainingAgentMapper(jdbc, new ObjectMapper());

        assertThat(mapper.findScanCursor("KNOWLEDGE")).isNull();

        assertThat(jdbc.sql).contains("training_source_scan_cursor")
                .doesNotContain("interview_", "algorithm_", "simulation_");
        assertThat(jdbc.args).containsExactly("KNOWLEDGE");
    }

    @Test
    void staleProcessingRecoveryBindsRetryTimeBeforeLeaseCutoff() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        TrainingAgentMapper mapper = new TrainingAgentMapper(jdbc, new ObjectMapper());
        LocalDateTime retryAt = LocalDateTime.of(2026, 8, 19, 10, 0);
        LocalDateTime staleBefore = retryAt.minusMinutes(5);

        assertThat(mapper.recoverStaleProcessing(staleBefore, retryAt)).isEqualTo(1);

        assertThat(jdbc.sql).contains("status='PROCESSING'", "last_attempt_at<?");
        assertThat(jdbc.args).containsExactly(retryAt, staleBefore);
    }

    @Test
    void duplicateRegistrationCanBackfillCompletionTimeWithoutCountingAsNewDiscovery() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        jdbc.updateResult = 2;
        TrainingAgentMapper mapper = new TrainingAgentMapper(jdbc, new ObjectMapper());
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 19, 9, 0);

        boolean inserted = mapper.registerSource(new CompletedTrainingReportRef(
                "ALGORITHM", 7L, 31L, 1, completedAt));

        assertThat(inserted).isFalse();
        assertThat(jdbc.sql).contains("source_completed_at",
                "COALESCE(source_completed_at, VALUES(source_completed_at))");
        assertThat(jdbc.args).containsExactly("ALGORITHM", 31L, 1, 7L, completedAt);
    }

    @Test
    void projectionLockScopesTheDatabaseLockToOneUser() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        TrainingAgentMapper mapper = new TrainingAgentMapper(jdbc, new ObjectMapper());

        mapper.lockUserProjection(7L);

        assertThat(jdbc.sql).isEqualTo(
                "SELECT user_id FROM training_agent_user_lock WHERE user_id=? FOR UPDATE");
        assertThat(jdbc.args).containsExactly(7L);
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private String sql;
        private Object[] args;
        private int updateResult = 1;

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.sql = sql;
            this.args = args;
            return List.of();
        }

        @Override
        public int update(String sql, Object... args) {
            this.sql = sql;
            this.args = args;
            return updateResult;
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            this.sql = sql;
            this.args = args;
            return requiredType.cast(7L);
        }
    }
}
