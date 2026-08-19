package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.trainingagent.domain.AbilityEvidence;
import com.agent2026.interview.trainingagent.domain.AbilityProfileAggregator;
import com.agent2026.interview.trainingagent.domain.AbilitySnapshot;
import com.agent2026.interview.trainingagent.domain.TrainingHistorySignal;
import com.agent2026.interview.trainingagent.domain.TrainingRecommendation;
import com.agent2026.interview.trainingagent.domain.TrainingRecommendationPolicy;
import com.agent2026.interview.trainingagent.infrastructure.persistence.StoredRecommendationRow;
import com.agent2026.interview.trainingagent.infrastructure.persistence.TrainingAgentMapper;
import com.agent2026.interview.trainingagent.infrastructure.source.TrainingReportSourceCatalog;
import com.agent2026.interview.trainingagent.infrastructure.metrics.TrainingAgentMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TrainingAgentProjectionService {
    private static final Logger log = LoggerFactory.getLogger(TrainingAgentProjectionService.class);
    private static final TypeReference<List<TrainingRecommendation.Item>> ITEM_LIST = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> ACTION_MAP = new TypeReference<>() {};
    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() {};
    private final TrainingAgentMapper mapper;
    private final TrainingReportSourceCatalog sources;
    private final AbilityProfileAggregator aggregator;
    private final TrainingRecommendationPolicy policy;
    private final ObjectMapper objectMapper;
    private final TrainingAgentMetrics metrics;
    private final TrainingAgentUserLockService userLocks;

    public TrainingAgentProjectionService(TrainingAgentMapper mapper, TrainingReportSourceCatalog sources,
                                        AbilityProfileAggregator aggregator,
                                        TrainingRecommendationPolicy policy, ObjectMapper objectMapper,
                                        TrainingAgentMetrics metrics, TrainingAgentUserLockService userLocks) {
        this.mapper = mapper;
        this.sources = sources;
        this.aggregator = aggregator;
        this.policy = policy;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.userLocks = userLocks;
    }

    @Transactional
    public Projection recompute(Long userId, LocalDateTime now) {
        return metrics.timeProjection(() -> recomputeInternal(userId, now));
    }

    private Projection recomputeInternal(Long userId, LocalDateTime now) {
        long startedAt = System.nanoTime();
        userLocks.ensure(userId);
        mapper.lockUserProjection(userId);
        List<AbilityEvidence> evidence = mapper.findEvidence(userId);
        List<AbilitySnapshot> snapshots = aggregator.aggregate(evidence, now);
        mapper.upsertSnapshots(userId, snapshots, AbilityProfileAggregator.POLICY_VERSION);
        TrainingHistorySignal history = new TrainingHistorySignal(sources.recentTrainingTypes(userId, 2));
        TrainingRecommendation recommendation = policy.recommend(snapshots, evidence, history, now);
        long revision = persist(userId, recommendation, now);
        long durationMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        log.info("training agent projection recomputed: user={}, evidence={}, state={}, revision={}, durationMs={}",
                userId, evidence.size(), recommendation.state(), revision, durationMillis);
        return new Projection(revision, recommendation, snapshots, now);
    }

    public Optional<Projection> findReusable(Long userId, LocalDateTime now) {
        if (!mapper.snapshotsUsePolicy(userId, AbilityProfileAggregator.POLICY_VERSION,
                com.agent2026.interview.trainingagent.domain.AbilityDimension.values().length)) {
            return Optional.empty();
        }
        return mapper.findRecommendation(userId)
                .filter(row -> TrainingRecommendationPolicy.POLICY_VERSION.equals(row.policyVersion()))
                .filter(row -> row.expiresAt() != null && row.expiresAt().isAfter(now))
                .map(row -> restore(row, mapper.findSnapshots(userId)));
    }

    private long persist(Long userId, TrainingRecommendation recommendation, LocalDateTime now) {
        TrainingRecommendation.Item primary = recommendation.primary();
        try {
            return mapper.upsertRecommendation(userId, recommendation.state(), primary.trainingType(),
                    primary.dimensionCode(), primary.title(), primary.reason(), primary.estimatedMinutes(),
                    objectMapper.writeValueAsString(primary.action()),
                    objectMapper.writeValueAsString(recommendation.alternatives()),
                    objectMapper.writeValueAsString(primary.evidenceIds()), TrainingRecommendationPolicy.POLICY_VERSION,
                    now, now.plusHours(24));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("training agent recommendation serialization failed", ex);
        }
    }

    private Projection restore(StoredRecommendationRow row, List<AbilitySnapshot> snapshots) {
        try {
            Map<String, Object> action = row.actionJson() == null ? Map.of()
                    : objectMapper.readValue(row.actionJson(), ACTION_MAP);
            List<Long> evidenceIds = row.evidenceIdsJson() == null ? List.of()
                    : objectMapper.readValue(row.evidenceIdsJson(), LONG_LIST);
            TrainingRecommendation.Item primary = new TrainingRecommendation.Item(row.trainingType(),
                    row.dimensionCode(), row.title(), row.reason(), row.estimatedMinutes(), action, evidenceIds);
            List<TrainingRecommendation.Item> alternatives = row.alternativesJson() == null ? List.of()
                    : objectMapper.readValue(row.alternativesJson(), ITEM_LIST);
            return new Projection(row.revision(), new TrainingRecommendation(row.state(), primary, alternatives),
                    snapshots, row.generatedAt());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("stored training recommendation is invalid", ex);
        }
    }

    public record Projection(long revision, TrainingRecommendation recommendation,
                             List<AbilitySnapshot> snapshots, LocalDateTime generatedAt) {}
}
