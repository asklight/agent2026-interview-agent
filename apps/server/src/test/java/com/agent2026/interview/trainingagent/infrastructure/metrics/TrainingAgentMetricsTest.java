package com.agent2026.interview.trainingagent.infrastructure.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingAgentMetricsTest {

    @Test
    void recordsBoundedSyncResultTagsAndCollapsesUnknownSources() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TrainingAgentMetrics metrics = new TrainingAgentMetrics(registry);

        metrics.recordSyncResult("PROJECT_DEEP_DIVE", TrainingAgentMetrics.SyncResult.COMPLETED);
        metrics.recordSyncResult("session-12345", TrainingAgentMetrics.SyncResult.FAILED);

        assertThat(registry.get(TrainingAgentMetrics.SYNC_TOTAL)
                .tags("source_type", "project_deep_dive", "result", "completed")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get(TrainingAgentMetrics.SYNC_TOTAL)
                .tags("source_type", "unknown", "result", "failed")
                .counter().count()).isEqualTo(1);
    }

    @Test
    void recordsDelayConflictProjectionDashboardAndPendingMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TrainingAgentMetrics metrics = new TrainingAgentMetrics(registry);

        metrics.recordSyncDelay(Duration.ofMillis(250));
        metrics.recordEvidenceIdempotentConflict();
        assertThat(metrics.timeProjection(() -> "projection")).isEqualTo("projection");
        metrics.timeProjection(() -> { });
        metrics.recordDashboardState(TrainingAgentMetrics.DashboardState.COLD_START);
        metrics.updatePending(12);

        assertThat(registry.get(TrainingAgentMetrics.SYNC_DELAY).timer().count()).isEqualTo(1);
        assertThat(registry.get(TrainingAgentMetrics.SYNC_DELAY).timer().totalTime(
                java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(250);
        assertThat(registry.get(TrainingAgentMetrics.EVIDENCE_IDEMPOTENT_CONFLICTS)
                .counter().count()).isEqualTo(1);
        assertThat(registry.get(TrainingAgentMetrics.PROJECTION_DURATION).timer().count()).isEqualTo(2);
        assertThat(registry.get(TrainingAgentMetrics.DASHBOARD_TOTAL)
                .tag("state", "cold_start").counter().count()).isEqualTo(1);
        assertThat(registry.get(TrainingAgentMetrics.SYNC_PENDING).gauge().value()).isEqualTo(12);

        metrics.updatePending(-3);
        assertThat(registry.get(TrainingAgentMetrics.SYNC_PENDING).gauge().value()).isZero();
    }

    @Test
    void neverCreatesIdentityTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TrainingAgentMetrics metrics = new TrainingAgentMetrics(registry);
        metrics.recordSyncResult("KNOWLEDGE", TrainingAgentMetrics.SyncResult.REJECTED);
        metrics.recordDashboardState(TrainingAgentMetrics.DashboardState.DEGRADED);

        Set<String> forbiddenTags = Set.of("userId", "user_id", "sessionId", "session_id");
        assertThat(registry.getMeters())
                .flatMap(meter -> meter.getId().getTags())
                .extracting(io.micrometer.core.instrument.Tag::getKey)
                .noneMatch(forbiddenTags::contains);
        assertThat(registry.getMeters()).extracting(Meter::getId)
                .allSatisfy(id -> assertThat(id.getTag("userId")).isNull());
    }

    @Test
    void rejectsNegativeSyncDelay() {
        TrainingAgentMetrics metrics = new TrainingAgentMetrics(new SimpleMeterRegistry());

        assertThatThrownBy(() -> metrics.recordSyncDelay(Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
