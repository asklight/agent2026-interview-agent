package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.trainingagent.infrastructure.persistence.TrainingAgentMapper;
import com.agent2026.interview.trainingagent.infrastructure.metrics.TrainingAgentMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class TrainingAgentRecoveryScheduler {
    private static final Logger log = LoggerFactory.getLogger(TrainingAgentRecoveryScheduler.class);
    private final EvidenceSynchronizationService synchronization;
    private final TrainingAgentMapper mapper;
    private final Clock clock;
    private final boolean enabled;
    private final int batchSize;
    private final TrainingAgentMetrics metrics;

    public TrainingAgentRecoveryScheduler(EvidenceSynchronizationService synchronization, TrainingAgentMapper mapper,
                                          Clock clock,
                                          @Value("${training-agent.enabled:true}") boolean enabled,
                                          @Value("${training-agent.recovery.batch-size:100}") int batchSize,
                                          TrainingAgentMetrics metrics) {
        this.synchronization = synchronization;
        this.mapper = mapper;
        this.clock = clock;
        this.enabled = enabled;
        this.batchSize = Math.max(1, Math.min(200, batchSize));
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${training-agent.recovery.delay-ms:60000}",
            initialDelayString = "${training-agent.recovery.initial-delay-ms:15000}")
    public void recover() {
        if (!enabled) return;
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        int recovered = mapper.recoverStaleProcessing(now.minusMinutes(5), now);
        if (recovered > 0) {
            log.warn("training evidence processing leases recovered: count={}", recovered);
        }
        synchronization.scheduledBackfill(batchSize);
        metrics.updatePending(mapper.countPendingSources());
    }
}
