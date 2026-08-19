package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.trainingagent.infrastructure.persistence.TrainingAgentMapper;
import com.agent2026.interview.trainingagent.infrastructure.metrics.TrainingAgentMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TrainingAgentRecoverySchedulerTest {
    private static final Instant INSTANT = Instant.parse("2026-08-19T02:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZoneOffset.UTC);

    private final EvidenceSynchronizationService synchronization = mock(EvidenceSynchronizationService.class);
    private final TrainingAgentMapper mapper = mock(TrainingAgentMapper.class);
    private final Clock clock = Clock.fixed(INSTANT, ZoneOffset.UTC);

    @Test
    void recoversExpiredProcessingLeasesBeforeStartingBoundedBackfill() {
        TrainingAgentRecoveryScheduler scheduler = new TrainingAgentRecoveryScheduler(
                synchronization, mapper, clock, true, 500,
                new TrainingAgentMetrics(new SimpleMeterRegistry()));

        scheduler.recover();

        verify(mapper).recoverStaleProcessing(NOW.minusMinutes(5), NOW);
        verify(synchronization).scheduledBackfill(200);
    }

    @Test
    void disabledSchedulerDoesNotTouchSynchronizationState() {
        new TrainingAgentRecoveryScheduler(synchronization, mapper, clock, false, 100,
                new TrainingAgentMetrics(new SimpleMeterRegistry())).recover();

        verifyNoInteractions(mapper, synchronization);
    }
}
