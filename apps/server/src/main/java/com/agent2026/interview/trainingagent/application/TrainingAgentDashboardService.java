package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.trainingagent.api.TrainingAgentDashboardResponse;
import com.agent2026.interview.trainingagent.domain.AbilityEvidence;
import com.agent2026.interview.trainingagent.domain.AbilityProfileAggregator;
import com.agent2026.interview.trainingagent.domain.AbilitySnapshot;
import com.agent2026.interview.trainingagent.domain.TrainingRecommendation;
import com.agent2026.interview.trainingagent.domain.TrainingRecommendationPolicy;
import com.agent2026.interview.trainingagent.infrastructure.persistence.TrainingAgentMapper;
import com.agent2026.interview.trainingagent.infrastructure.persistence.TrainingAgentSourceRow;
import com.agent2026.interview.trainingagent.infrastructure.source.TrainingEvidenceExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final TrainingAgentMapper mapper;
    private final TrainingEvidenceExtractor extractor;
    private final AbilityProfileAggregator aggregator;
    private final TrainingRecommendationPolicy policy;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final boolean enabled;
    private final int maxSourceSync;

    public TrainingAgentDashboardService(TrainingAgentMapper mapper, TrainingEvidenceExtractor extractor,
                                         AbilityProfileAggregator aggregator, TrainingRecommendationPolicy policy,
                                         ObjectMapper objectMapper, Clock clock,
                                         @Value("${training-agent.enabled:true}") boolean enabled,
                                         @Value("${training-agent.max-source-sync:100}") int maxSourceSync) {
        this.mapper = mapper;
        this.extractor = extractor;
        this.aggregator = aggregator;
        this.policy = policy;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.enabled = enabled;
        this.maxSourceSync = Math.max(1, Math.min(500, maxSourceSync));
    }

    public TrainingAgentDashboardResponse dashboard(Long userId) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (!enabled) return new TrainingAgentDashboardResponse(false, false, "DISABLED", null, List.of(), List.of(), now);
        try {
            List<TrainingAgentSourceRow> sources = mapper.findCompletedReports(userId, maxSourceSync);
            for (TrainingAgentSourceRow source : sources) {
                for (AbilityEvidence evidence : extractor.extract(source, now)) mapper.insertEvidenceIgnore(evidence);
            }
            List<AbilityEvidence> evidence = mapper.findEvidence(userId);
            List<AbilitySnapshot> snapshots = aggregator.aggregate(evidence, now);
            mapper.upsertSnapshots(userId, snapshots, AbilityProfileAggregator.POLICY_VERSION);
            TrainingRecommendation recommendation = policy.recommend(snapshots, evidence, now);
            persistRecommendation(userId, recommendation, now);
            return toResponse(recommendation, snapshots, now, false);
        } catch (RuntimeException ex) {
            log.warn("training agent dashboard degraded for user {}", userId, ex);
            return degraded(now);
        }
    }

    private void persistRecommendation(Long userId, TrainingRecommendation recommendation, LocalDateTime now) {
        TrainingRecommendation.Item primary = recommendation.primary();
        try {
            mapper.upsertRecommendation(userId, recommendation.state(), primary.trainingType(), primary.dimensionCode(),
                    primary.title(), primary.reason(), objectMapper.writeValueAsString(primary.action()),
                    objectMapper.writeValueAsString(recommendation.alternatives()),
                    objectMapper.writeValueAsString(primary.evidenceIds()), TrainingRecommendationPolicy.POLICY_VERSION,
                    now, now.plusHours(24));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("training agent recommendation serialization failed", ex);
        }
    }

    private TrainingAgentDashboardResponse toResponse(TrainingRecommendation recommendation,
                                                       List<AbilitySnapshot> snapshots, LocalDateTime now,
                                                       boolean degraded) {
        return new TrainingAgentDashboardResponse(true, degraded, recommendation.state(), item(recommendation.primary()),
                recommendation.alternatives().stream().map(this::item).toList(), focus(snapshots), now);
    }

    private TrainingAgentDashboardResponse.RecommendationItem item(TrainingRecommendation.Item value) {
        return new TrainingAgentDashboardResponse.RecommendationItem(value.trainingType(), value.dimensionCode(),
                value.title(), value.reason(), value.estimatedMinutes(), value.action(), value.evidenceIds());
    }

    private List<TrainingAgentDashboardResponse.AbilityFocus> focus(List<AbilitySnapshot> snapshots) {
        return snapshots.stream()
                .filter(item -> item.state().needsTraining())
                .sorted(Comparator.comparingInt((AbilitySnapshot item) -> item.state().priority()).reversed()
                        .thenComparingDouble(AbilitySnapshot::confidence))
                .limit(3)
                .map(item -> new TrainingAgentDashboardResponse.AbilityFocus(item.dimension().code(), item.dimension().label(),
                        item.dimension().sourceType(), item.state().name(), item.confidence(), item.gapCount(),
                        item.riskCount(), item.lastObservedAt()))
                .toList();
    }

    private TrainingAgentDashboardResponse degraded(LocalDateTime now) {
        return new TrainingAgentDashboardResponse(true, true, "DEGRADED", null, List.of(), List.of(), now);
    }
}
