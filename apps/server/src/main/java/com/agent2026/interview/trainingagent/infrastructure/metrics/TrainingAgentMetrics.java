package com.agent2026.interview.trainingagent.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class TrainingAgentMetrics {
    public static final String SYNC_TOTAL = "training.agent.sync.total";
    public static final String SYNC_DELAY = "training.agent.sync.delay";
    public static final String EVIDENCE_IDEMPOTENT_CONFLICTS =
            "training.agent.evidence.idempotent.conflicts";
    public static final String PROJECTION_DURATION = "training.agent.projection.duration";
    public static final String DASHBOARD_TOTAL = "training.agent.dashboard.total";
    public static final String SYNC_PENDING = "training.agent.sync.pending";

    private static final Set<String> SOURCE_TYPES = Set.of(
            "KNOWLEDGE", "PROJECT_DEEP_DIVE", "ALGORITHM", "COMPREHENSIVE_SIMULATION");

    private final MeterRegistry registry;
    private final Timer syncDelay;
    private final Counter evidenceIdempotentConflicts;
    private final Timer projectionDuration;
    private final AtomicInteger pending = new AtomicInteger();

    public TrainingAgentMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.syncDelay = Timer.builder(SYNC_DELAY)
                .description("Delay between training completion and evidence synchronization")
                .register(registry);
        this.evidenceIdempotentConflicts = Counter.builder(EVIDENCE_IDEMPOTENT_CONFLICTS)
                .description("Evidence inserts ignored by the idempotency constraint")
                .register(registry);
        this.projectionDuration = Timer.builder(PROJECTION_DURATION)
                .description("Ability snapshot and recommendation projection duration")
                .register(registry);
        Gauge.builder(SYNC_PENDING, pending, AtomicInteger::get)
                .description("Training evidence sources waiting for synchronization")
                .register(registry);
    }

    public void recordSyncResult(String sourceType, SyncResult result) {
        registry.counter(SYNC_TOTAL,
                "source_type", normalizeSourceType(sourceType),
                "result", Objects.requireNonNull(result, "result").tagValue())
                .increment();
    }

    public void recordSyncDelay(Duration delay) {
        Objects.requireNonNull(delay, "delay");
        if (delay.isNegative()) throw new IllegalArgumentException("sync delay cannot be negative");
        syncDelay.record(delay);
    }

    public void recordEvidenceIdempotentConflict() {
        evidenceIdempotentConflicts.increment();
    }

    public <T> T timeProjection(Supplier<T> operation) {
        return projectionDuration.record(Objects.requireNonNull(operation, "operation"));
    }

    public void timeProjection(Runnable operation) {
        projectionDuration.record(Objects.requireNonNull(operation, "operation"));
    }

    public void recordDashboardState(DashboardState state) {
        registry.counter(DASHBOARD_TOTAL,
                "state", Objects.requireNonNull(state, "state").tagValue())
                .increment();
    }

    public void updatePending(int count) {
        pending.set(Math.max(0, count));
    }

    private String normalizeSourceType(String sourceType) {
        if (sourceType == null) return "unknown";
        String normalized = sourceType.trim().toUpperCase(Locale.ROOT);
        return SOURCE_TYPES.contains(normalized) ? normalized.toLowerCase(Locale.ROOT) : "unknown";
    }

    public enum SyncResult {
        COMPLETED,
        FAILED,
        REJECTED,
        SKIPPED;

        private String tagValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum DashboardState {
        READY,
        COLD_START,
        DEGRADED,
        DISABLED;

        private String tagValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
