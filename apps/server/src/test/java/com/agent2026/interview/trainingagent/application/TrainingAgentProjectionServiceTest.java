package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.trainingagent.domain.AbilityDimension;
import com.agent2026.interview.trainingagent.domain.AbilityEvidence;
import com.agent2026.interview.trainingagent.domain.AbilityProfileAggregator;
import com.agent2026.interview.trainingagent.domain.AbilitySnapshot;
import com.agent2026.interview.trainingagent.domain.AbilityState;
import com.agent2026.interview.trainingagent.domain.EvidencePolarity;
import com.agent2026.interview.trainingagent.domain.TrainingHistorySignal;
import com.agent2026.interview.trainingagent.domain.TrainingRecommendation;
import com.agent2026.interview.trainingagent.domain.TrainingRecommendationPolicy;
import com.agent2026.interview.trainingagent.infrastructure.persistence.StoredRecommendationRow;
import com.agent2026.interview.trainingagent.infrastructure.persistence.TrainingAgentMapper;
import com.agent2026.interview.trainingagent.infrastructure.source.TrainingReportSourceCatalog;
import com.agent2026.interview.trainingagent.infrastructure.metrics.TrainingAgentMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainingAgentProjectionServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 19, 10, 0);

    private final TrainingAgentMapper mapper = mock(TrainingAgentMapper.class);
    private final TrainingReportSourceCatalog sources = mock(TrainingReportSourceCatalog.class);
    private final AbilityProfileAggregator aggregator = mock(AbilityProfileAggregator.class);
    private final TrainingRecommendationPolicy policy = mock(TrainingRecommendationPolicy.class);
    private final TrainingAgentUserLockService userLocks = mock(TrainingAgentUserLockService.class);
    private final TrainingAgentProjectionService service = new TrainingAgentProjectionService(
            mapper, sources, aggregator, policy, new ObjectMapper(),
            new TrainingAgentMetrics(new SimpleMeterRegistry()), userLocks);

    @Test
    void restoresValidProjectionWithoutRecomputingEvidence() {
        StoredRecommendationRow row = storedRecommendation(
                TrainingRecommendationPolicy.POLICY_VERSION, NOW.plusHours(1));
        List<AbilitySnapshot> snapshots = List.of(new AbilitySnapshot(AbilityDimension.KNOWLEDGE_JAVA,
                AbilityState.NEEDS_WORK, -2, 0.7, 0, 2, 0, 2, NOW.minusMinutes(10)));
        when(mapper.snapshotsUsePolicy(7L, AbilityProfileAggregator.POLICY_VERSION,
                AbilityDimension.values().length)).thenReturn(true);
        when(mapper.findRecommendation(7L)).thenReturn(Optional.of(row));
        when(mapper.findSnapshots(7L)).thenReturn(snapshots);

        Optional<TrainingAgentProjectionService.Projection> result = service.findReusable(7L, NOW);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().revision()).isEqualTo(4L);
        assertThat(result.orElseThrow().recommendation().primary().action())
                .containsEntry("module", "Java");
        assertThat(result.orElseThrow().snapshots()).isEqualTo(snapshots);
        verify(mapper, never()).findEvidence(7L);
    }

    @Test
    void expiredRecommendationIsNotReusableAndDoesNotLoadSnapshots() {
        when(mapper.snapshotsUsePolicy(7L, AbilityProfileAggregator.POLICY_VERSION,
                AbilityDimension.values().length)).thenReturn(true);
        when(mapper.findRecommendation(7L)).thenReturn(Optional.of(storedRecommendation(
                TrainingRecommendationPolicy.POLICY_VERSION, NOW)));

        assertThat(service.findReusable(7L, NOW)).isEmpty();

        verify(mapper, never()).findSnapshots(7L);
    }

    @Test
    void outdatedSnapshotPolicyStopsCacheLookupBeforeReadingRecommendation() {
        when(mapper.snapshotsUsePolicy(7L, AbilityProfileAggregator.POLICY_VERSION,
                AbilityDimension.values().length)).thenReturn(false);

        assertThat(service.findReusable(7L, NOW)).isEmpty();

        verify(mapper, never()).findRecommendation(7L);
        verify(mapper, never()).findSnapshots(7L);
    }

    @Test
    void recomputeHasAnAtomicTransactionBoundary() throws Exception {
        Method method = TrainingAgentProjectionService.class
                .getDeclaredMethod("recompute", Long.class, LocalDateTime.class);

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }

    @Test
    void recomputeLocksTheCurrentUserBeforeReadingProjectionEvidence() {
        AbilityEvidence evidence = new AbilityEvidence(101L, 7L, "KNOWLEDGE", 11L, 12L, 1,
                "key", AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.GAP, 3, 0.8,
                "Java 边界说明不足", null, null, Map.of(), NOW.minusMinutes(5));
        AbilitySnapshot snapshot = new AbilitySnapshot(AbilityDimension.KNOWLEDGE_JAVA,
                AbilityState.NEEDS_WORK, -2.4, 0.8, 0, 1, 0, 1, NOW.minusMinutes(5));
        TrainingHistorySignal history = new TrainingHistorySignal(List.of("KNOWLEDGE", "ALGORITHM"));
        TrainingRecommendation recommendation = new TrainingRecommendation("READY",
                new TrainingRecommendation.Item("KNOWLEDGE", "KNOWLEDGE.JAVA", "Java 校准",
                        "最近一次训练暴露了边界问题", 10,
                        Map.of("module", "Java", "questionCount", 3), List.of(101L)),
                List.of());
        when(mapper.findEvidence(7L)).thenReturn(List.of(evidence));
        when(aggregator.aggregate(List.of(evidence), NOW)).thenReturn(List.of(snapshot));
        when(sources.recentTrainingTypes(7L, 2)).thenReturn(history.recentTrainingTypes());
        when(policy.recommend(List.of(snapshot), List.of(evidence), history, NOW)).thenReturn(recommendation);
        when(mapper.upsertRecommendation(eq(7L), eq("READY"), eq("KNOWLEDGE"),
                eq("KNOWLEDGE.JAVA"), eq("Java 校准"), eq("最近一次训练暴露了边界问题"), eq(10),
                anyString(), anyString(), anyString(), eq(TrainingRecommendationPolicy.POLICY_VERSION),
                eq(NOW), eq(NOW.plusHours(24)))).thenReturn(5L);

        TrainingAgentProjectionService.Projection result = service.recompute(7L, NOW);

        var mapperOrder = inOrder(userLocks, mapper);
        mapperOrder.verify(userLocks).ensure(7L);
        mapperOrder.verify(mapper).lockUserProjection(7L);
        mapperOrder.verify(mapper).findEvidence(7L);
        assertThat(result.revision()).isEqualTo(5L);
        assertThat(result.recommendation()).isEqualTo(recommendation);
    }

    private StoredRecommendationRow storedRecommendation(String policyVersion, LocalDateTime expiresAt) {
        return new StoredRecommendationRow(4L, "READY", "KNOWLEDGE", "KNOWLEDGE.JAVA",
                "Java quick calibration", "Recent Java gap", 10,
                "{\"module\":\"Java\"}", "[]", "[101]", policyVersion,
                NOW.minusMinutes(2), expiresAt);
    }
}
