package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.trainingagent.api.TrainingAgentDashboardResponse;
import com.agent2026.interview.trainingagent.domain.AbilitySnapshot;
import com.agent2026.interview.trainingagent.domain.AbilityState;
import com.agent2026.interview.trainingagent.domain.TrainingRecommendation;
import com.agent2026.interview.trainingagent.infrastructure.metrics.TrainingAgentMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Service
public class TrainingAgentDashboardService {
    private static final Logger log = LoggerFactory.getLogger(TrainingAgentDashboardService.class);
    private final EvidenceSynchronizationService synchronization;
    private final TrainingAgentProjectionService projections;
    private final Clock clock;
    private final boolean enabled;
    private final int maxSourceSync;
    private final TrainingAgentMetrics metrics;

    public TrainingAgentDashboardService(EvidenceSynchronizationService synchronization,
                                         TrainingAgentProjectionService projections, Clock clock,
                                         @Value("${training-agent.enabled:true}") boolean enabled,
                                         @Value("${training-agent.max-source-sync:10}") int maxSourceSync,
                                         TrainingAgentMetrics metrics) {
        this.synchronization = synchronization;
        this.projections = projections;
        this.clock = clock;
        this.enabled = enabled;
        this.maxSourceSync = Math.max(1, Math.min(10, maxSourceSync));
        this.metrics = metrics;
    }

    public TrainingAgentDashboardResponse dashboard(Long userId) {
        LocalDateTime now = now();
        if (!enabled) return disabled(now);
        try {
            synchronization.lightweightBackfill(userId, maxSourceSync);
            TrainingAgentProjectionService.Projection projection = projections.findReusable(userId, now)
                    .orElseGet(() -> projections.recompute(userId, now));
            return response(projection, false);
        } catch (RuntimeException ex) {
            log.warn("training agent dashboard degraded for user {}", userId, ex);
            try {
                return projections.findReusable(userId, now)
                        .map(projection -> response(projection, true))
                        .orElseGet(() -> degraded(now));
            } catch (RuntimeException fallbackFailure) {
                log.warn("training agent dashboard fallback unavailable for user {}", userId, fallbackFailure);
                return degraded(now);
            }
        }
    }

    private TrainingAgentDashboardResponse response(TrainingAgentProjectionService.Projection projection,
                                                     boolean degraded) {
        metrics.recordDashboardState(degraded ? TrainingAgentMetrics.DashboardState.DEGRADED
                : dashboardState(projection.recommendation().state()));
        TrainingRecommendation recommendation = projection.recommendation();
        List<AbilitySnapshot> snapshots = projection.snapshots();
        return new TrainingAgentDashboardResponse(true, degraded, recommendation.state(), focus(snapshots),
                item(projection.revision(), recommendation.primary()),
                recommendation.alternatives().stream().map(value -> item(projection.revision(), value)).toList(),
                progress(snapshots), projection.generatedAt());
    }

    private TrainingAgentDashboardResponse.RecommendationItem item(long revision,
                                                                    TrainingRecommendation.Item value) {
        return new TrainingAgentDashboardResponse.RecommendationItem(revision, value.trainingType(),
                value.dimensionCode(), value.title(), value.reason(), value.estimatedMinutes(), value.action(),
                value.evidenceIds().size());
    }

    private List<TrainingAgentDashboardResponse.AbilityFocus> focus(List<AbilitySnapshot> snapshots) {
        return snapshots.stream()
                .filter(item -> item.state().needsTraining())
                .sorted(Comparator.comparingInt((AbilitySnapshot item) -> item.state().priority()).reversed()
                        .thenComparing(AbilitySnapshot::lastObservedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(item -> item.dimension().code()))
                .limit(3)
                .map(item -> new TrainingAgentDashboardResponse.AbilityFocus(item.dimension().code(),
                        item.dimension().label(), item.dimension().sourceType(), item.state().name(),
                        item.strengthCount() + item.gapCount() + item.riskCount(), item.lastObservedAt()))
                .toList();
    }

    private TrainingAgentDashboardResponse.RecentProgress progress(List<AbilitySnapshot> snapshots) {
        return snapshots.stream()
                .filter(item -> item.state() == AbilityState.STABLE || item.state() == AbilityState.STRONG)
                .filter(item -> item.lastObservedAt() != null)
                .max(Comparator.comparing(AbilitySnapshot::lastObservedAt)
                        .thenComparing(item -> item.dimension().code()))
                .map(item -> new TrainingAgentDashboardResponse.RecentProgress(item.dimension().code(),
                        item.dimension().label(), item.dimension().sourceType(), item.state().name(),
                        item.lastObservedAt()))
                .orElse(null);
    }

    private TrainingAgentDashboardResponse disabled(LocalDateTime now) {
        metrics.recordDashboardState(TrainingAgentMetrics.DashboardState.DISABLED);
        return new TrainingAgentDashboardResponse(false, false, "DISABLED", List.of(), null, List.of(), null, now);
    }

    private TrainingAgentDashboardResponse degraded(LocalDateTime now) {
        metrics.recordDashboardState(TrainingAgentMetrics.DashboardState.DEGRADED);
        return new TrainingAgentDashboardResponse(true, true, "DEGRADED", List.of(), null, List.of(), null, now);
    }

    private TrainingAgentMetrics.DashboardState dashboardState(String state) {
        return "READY".equals(state) ? TrainingAgentMetrics.DashboardState.READY
                : TrainingAgentMetrics.DashboardState.COLD_START;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
