package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.trainingagent.api.TrainingAgentDashboardResponse;
import com.agent2026.interview.trainingagent.domain.AbilityDimension;
import com.agent2026.interview.trainingagent.domain.AbilitySnapshot;
import com.agent2026.interview.trainingagent.domain.AbilityState;
import com.agent2026.interview.trainingagent.domain.TrainingRecommendation;
import com.agent2026.interview.trainingagent.infrastructure.metrics.TrainingAgentMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrainingAgentDashboardServiceTest {
    private static final Instant INSTANT = Instant.parse("2026-08-19T02:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZoneOffset.UTC);
    private static final LocalDateTime GENERATED_AT = NOW.minusMinutes(2);

    private final EvidenceSynchronizationService synchronization = mock(EvidenceSynchronizationService.class);
    private final TrainingAgentProjectionService projections = mock(TrainingAgentProjectionService.class);
    private final Clock clock = Clock.fixed(INSTANT, ZoneOffset.UTC);

    @Test
    void reusesValidProjectionAndClampsDashboardCompensationToTenSources() {
        TrainingAgentProjectionService.Projection projection = readyProjection();
        TrainingAgentDashboardService service = service(true, 100);
        when(projections.findReusable(7L, NOW)).thenReturn(Optional.of(projection));

        TrainingAgentDashboardResponse response = service.dashboard(7L);

        assertThat(response.degraded()).isFalse();
        assertThat(response.state()).isEqualTo("READY");
        assertThat(response.generatedAt()).isEqualTo(GENERATED_AT);
        assertThat(response.primaryRecommendation().revision()).isEqualTo(4L);
        assertThat(response.focusDimensions()).extracting(
                TrainingAgentDashboardResponse.AbilityFocus::dimensionCode)
                .containsExactly("KNOWLEDGE.JAVA");
        verify(synchronization).lightweightBackfill(7L, 10);
        verify(projections, never()).recompute(7L, NOW);
    }

    @Test
    void recomputesOnlyWhenThereIsNoValidProjection() {
        TrainingAgentProjectionService.Projection projection = readyProjection();
        TrainingAgentDashboardService service = service(true, 10);
        when(projections.findReusable(7L, NOW)).thenReturn(Optional.empty());
        when(projections.recompute(7L, NOW)).thenReturn(projection);

        TrainingAgentDashboardResponse response = service.dashboard(7L);

        assertThat(response.state()).isEqualTo("READY");
        verify(projections).recompute(7L, NOW);
    }

    @Test
    void compensationFailureReturnsReusableProjectionAsDegradedWithoutRecomputing() {
        TrainingAgentProjectionService.Projection projection = readyProjection();
        TrainingAgentDashboardService service = service(true, 10);
        doThrow(new IllegalStateException("sync temporarily unavailable"))
                .when(synchronization).lightweightBackfill(7L, 10);
        when(projections.findReusable(7L, NOW)).thenReturn(Optional.of(projection));

        TrainingAgentDashboardResponse response = service.dashboard(7L);

        assertThat(response.degraded()).isTrue();
        assertThat(response.state()).isEqualTo("READY");
        assertThat(response.primaryRecommendation()).isNotNull();
        assertThat(response.generatedAt()).isEqualTo(GENERATED_AT);
        verify(projections, never()).recompute(7L, NOW);
    }

    @Test
    void returnsPageLevelDegradedStateWhenSynchronizationAndFallbackAreUnavailable() {
        TrainingAgentDashboardService service = service(true, 10);
        doThrow(new IllegalStateException("sync temporarily unavailable"))
                .when(synchronization).lightweightBackfill(7L, 10);
        when(projections.findReusable(7L, NOW)).thenThrow(new IllegalStateException("storage unavailable"));

        TrainingAgentDashboardResponse response = service.dashboard(7L);

        assertThat(response.enabled()).isTrue();
        assertThat(response.degraded()).isTrue();
        assertThat(response.state()).isEqualTo("DEGRADED");
        assertThat(response.primaryRecommendation()).isNull();
        assertThat(response.focusDimensions()).isEmpty();
    }

    @Test
    void disabledAgentDoesNotTouchSynchronizationOrProjectionStorage() {
        TrainingAgentDashboardResponse response = service(false, 10).dashboard(7L);

        assertThat(response.enabled()).isFalse();
        assertThat(response.state()).isEqualTo("DISABLED");
        verifyNoInteractions(synchronization, projections);
    }

    private TrainingAgentDashboardService service(boolean enabled, int maxSourceSync) {
        return new TrainingAgentDashboardService(synchronization, projections, clock, enabled, maxSourceSync,
                new TrainingAgentMetrics(new SimpleMeterRegistry()));
    }

    private TrainingAgentProjectionService.Projection readyProjection() {
        TrainingRecommendation.Item primary = new TrainingRecommendation.Item(
                "KNOWLEDGE", "KNOWLEDGE.JAVA", "Java quick calibration", "Recent Java gap",
                10, Map.of("module", "Java", "difficulty", "mixed", "questionCount", 3), List.of(101L));
        TrainingRecommendation recommendation = new TrainingRecommendation("READY", primary, List.of());
        AbilitySnapshot needsWork = new AbilitySnapshot(AbilityDimension.KNOWLEDGE_JAVA,
                AbilityState.NEEDS_WORK, -3, 0.7, 0, 2, 0, 2, NOW.minusMinutes(5));
        return new TrainingAgentProjectionService.Projection(
                4L, recommendation, List.of(needsWork), GENERATED_AT);
    }
}
