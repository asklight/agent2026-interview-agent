package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.trainingagent.domain.AbilityDimension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "true")
class TrainingAgentProjectionMySqlTest {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transactions;
    @Autowired
    private TrainingAgentProjectionService projections;
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long userId : userIds) {
            jdbc.update("DELETE FROM training_ability_evidence WHERE user_id=?", userId);
            jdbc.update("DELETE FROM user_ability_snapshot WHERE user_id=?", userId);
            jdbc.update("DELETE FROM training_recommendation WHERE user_id=?", userId);
            jdbc.update("DELETE FROM training_evidence_sync_state WHERE user_id=?", userId);
            jdbc.update("DELETE FROM training_agent_user_lock WHERE user_id=?", userId);
            jdbc.update("DELETE FROM app_user WHERE id=?", userId);
        }
    }

    @Test
    void concurrentEvidenceWritesPreserveBothProjectionInputs() throws Exception {
        Long userId = createUser();
        jdbc.update("INSERT INTO training_agent_user_lock(user_id) VALUES (?)", userId);
        LocalDateTime observedAt = LocalDateTime.of(2026, 8, 19, 10, 0);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var first = submit(executor, userId, 910001L, "mysql-concurrent-a", observedAt, ready, start, "GAP");
            var second = submit(executor, userId, 910002L, "mysql-concurrent-b", observedAt, ready, start, "STRENGTH");
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);

            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM training_ability_evidence WHERE user_id=?", Integer.class, userId))
                    .isEqualTo(2);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM user_ability_snapshot WHERE user_id=?", Integer.class, userId))
                    .isEqualTo(AbilityDimension.values().length);
            assertThat(jdbc.queryForObject(
                    "SELECT recommendation_revision FROM training_recommendation WHERE user_id=?",
                    Long.class, userId)).isEqualTo(2L);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private java.util.concurrent.Future<?> submit(java.util.concurrent.ExecutorService executor, Long userId,
                                                    Long sessionId, String evidenceKey,
                                                    LocalDateTime observedAt, CountDownLatch ready,
                                                    CountDownLatch start, String polarity) {
        return executor.submit(() -> {
            try {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                transactions.executeWithoutResult(status -> {
                    jdbc.update("""
                            INSERT INTO training_ability_evidence
                              (user_id, source_type, source_session_id, source_report_version, evidence_key,
                               dimension_code, polarity, severity, confidence, evidence_text, observed_at)
                            VALUES (?, 'KNOWLEDGE', ?, 1, ?, ?, ?, 3, 0.8, ?, ?)
                            """, userId, sessionId, evidenceKey,
                            sessionId % 2 == 0 ? "KNOWLEDGE.MYSQL" : "KNOWLEDGE.JAVA",
                            polarity, evidenceKey, observedAt);
                    projections.recompute(userId, observedAt);
                });
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        });
    }

    private Long createUser() {
        String username = "training-agent-it-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_user(username, normalized_username, password_hash, status)
                VALUES (?, ?, 'integration-test-password-hash', 'ACTIVE')
                """, username, username.toLowerCase());
        Long id = jdbc.queryForObject("SELECT id FROM app_user WHERE normalized_username=?", Long.class,
                username.toLowerCase());
        userIds.add(id);
        return id;
    }

}
