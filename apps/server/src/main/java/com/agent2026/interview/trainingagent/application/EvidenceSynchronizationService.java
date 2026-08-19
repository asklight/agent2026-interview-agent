package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.trainingagent.domain.AbilityEvidence;
import com.agent2026.interview.shared.training.CompletedTrainingReport;
import com.agent2026.interview.shared.training.CompletedTrainingReportRef;
import com.agent2026.interview.shared.training.TrainingReportCursor;
import com.agent2026.interview.trainingagent.infrastructure.persistence.TrainingAgentMapper;
import com.agent2026.interview.trainingagent.infrastructure.source.EvidenceRejectedException;
import com.agent2026.interview.trainingagent.infrastructure.source.TrainingEvidenceExtractor;
import com.agent2026.interview.trainingagent.infrastructure.source.TrainingReportSourceCatalog;
import com.agent2026.interview.trainingagent.infrastructure.metrics.TrainingAgentMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvidenceSynchronizationService {
    private static final Logger log = LoggerFactory.getLogger(EvidenceSynchronizationService.class);
    private final TrainingAgentMapper mapper;
    private final TrainingReportSourceCatalog sources;
    private final TrainingEvidenceExtractor extractor;
    private final TrainingAgentProjectionService projections;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final TrainingAgentMetrics metrics;

    public EvidenceSynchronizationService(TrainingAgentMapper mapper, TrainingReportSourceCatalog sources,
                                          TrainingEvidenceExtractor extractor,
                                          TrainingAgentProjectionService projections,
                                          TransactionTemplate transactions, Clock clock,
                                          TrainingAgentMetrics metrics) {
        this.mapper = mapper;
        this.sources = sources;
        this.extractor = extractor;
        this.projections = projections;
        this.transactions = transactions;
        this.clock = clock;
        this.metrics = metrics;
    }

    public boolean registerAndSynchronize(CompletedTrainingReportRef ref) {
        mapper.registerSource(ref);
        LocalDateTime now = now();
        if (!mapper.claimSource(ref, now)) {
            metrics.recordSyncResult(ref.sourceType(), TrainingAgentMetrics.SyncResult.SKIPPED);
            log.debug("training evidence synchronization skipped: type={}, session={}, reason=not_claimed",
                    ref.sourceType(), ref.sourceSessionId());
            return false;
        }
        try {
            transactions.executeWithoutResult(status -> synchronizeClaimed(ref, now));
            long delaySeconds = ref.completedAt() == null ? 0
                    : Math.max(0, java.time.Duration.between(ref.completedAt(), now).toSeconds());
            if (ref.completedAt() != null) {
                metrics.recordSyncDelay(Duration.between(ref.completedAt(), now).isNegative()
                        ? Duration.ZERO : Duration.between(ref.completedAt(), now));
            }
            metrics.recordSyncResult(ref.sourceType(), TrainingAgentMetrics.SyncResult.COMPLETED);
            log.info("training evidence synchronized: type={}, session={}, user={}, delaySeconds={}",
                    ref.sourceType(), ref.sourceSessionId(), ref.userId(), delaySeconds);
            return true;
        } catch (EvidenceRejectedException ex) {
            mapper.markRejected(ref, ex.errorCode());
            metrics.recordSyncResult(ref.sourceType(), TrainingAgentMetrics.SyncResult.REJECTED);
            log.warn("training evidence rejected: type={}, session={}, code={}",
                    ref.sourceType(), ref.sourceSessionId(), ex.errorCode());
            return false;
        } catch (RuntimeException ex) {
            int attempts = mapper.attemptCount(ref);
            long retrySeconds = Math.min(3600, 30L * (1L << Math.min(7, attempts - 1)));
            mapper.markFailed(ref, now.plusSeconds(retrySeconds), stableErrorCode(ex));
            metrics.recordSyncResult(ref.sourceType(), TrainingAgentMetrics.SyncResult.FAILED);
            log.warn("training evidence synchronization failed: type={}, session={}, attempt={}",
                    ref.sourceType(), ref.sourceSessionId(), attempts, ex);
            return false;
        }
    }

    public int lightweightBackfill(Long userId, int limit) {
        int bounded = Math.max(1, Math.min(10, limit));
        int candidateLimit = Math.max(10, Math.min(30, bounded * 3));
        int discovered = 0;
        for (CompletedTrainingReportRef ref : sources.recent(userId, candidateLimit)) {
            if (discovered >= bounded) break;
            if (mapper.registerSource(ref)) discovered++;
        }
        Map<String, CompletedTrainingReportRef> refs = new LinkedHashMap<>();
        for (CompletedTrainingReportRef ref : mapper.findRetryableSources(userId, now(), bounded)) {
            refs.put(ref.sourceType() + ":" + ref.sourceSessionId() + ":" + ref.sourceReportVersion(), ref);
        }
        int completed = 0;
        for (CompletedTrainingReportRef ref : refs.values()) {
            if (registerAndSynchronize(ref)) completed++;
        }
        log.debug("training evidence lightweight backfill completed: user={}, discovered={}, retryable={}, completed={}",
                userId, discovered, refs.size(), completed);
        return completed;
    }

    public int scheduledBackfill(int limit) {
        int bounded = Math.max(1, Math.min(200, limit));
        int discovered = discoverScheduledSources(bounded);
        List<CompletedTrainingReportRef> retryable = mapper.findRetryableSources(null, now(), bounded);
        int completed = 0;
        for (CompletedTrainingReportRef ref : retryable) {
            if (registerAndSynchronize(ref)) completed++;
        }
        log.info("training evidence scheduled backfill completed: discovered={}, retryable={}, completed={}",
                discovered, retryable.size(), completed);
        return completed;
    }

    private int discoverScheduledSources(int limit) {
        List<String> sourceTypes = sources.sourceTypes();
        int remaining = Math.max(sourceTypes.size(), limit);
        int discovered = 0;
        for (int index = 0; index < sourceTypes.size(); index++) {
            String sourceType = sourceTypes.get(index);
            int remainingTypes = sourceTypes.size() - index;
            int quota = Math.max(1, remaining / remainingTypes);
            TrainingReportCursor cursor = mapper.findScanCursor(sourceType);
            List<CompletedTrainingReportRef> page = sources.scan(sourceType, cursor, quota);
            Integer inserted = transactions.execute(status -> {
                int count = 0;
                for (CompletedTrainingReportRef ref : page) {
                    if (mapper.registerSource(ref)) count++;
                }
                if (page.isEmpty()) {
                    mapper.resetScanCursor(sourceType);
                } else {
                    CompletedTrainingReportRef last = page.get(page.size() - 1);
                    mapper.advanceScanCursor(sourceType,
                            new TrainingReportCursor(last.completedAt(), last.sourceSessionId()));
                }
                return count;
            });
            discovered += inserted == null ? 0 : inserted;
            remaining -= page.size();
        }
        return discovered;
    }

    private void synchronizeClaimed(CompletedTrainingReportRef ref, LocalDateTime now) {
        if ("COMPREHENSIVE_SIMULATION".equals(ref.sourceType())) {
            List<CompletedTrainingReportRef> children = sources.findCompletedChildren(
                    ref.userId(), ref.sourceSessionId());
            if (children.isEmpty()) {
                throw new EvidenceRejectedException("SIMULATION_CHILDREN_MISSING",
                        "completed simulation has no completed child report references");
            }
            for (CompletedTrainingReportRef child : children) synchronizeChild(child, now);
        } else {
            extractAndStore(ref, now);
        }
        projections.recompute(ref.userId(), now);
        mapper.markCompleted(ref);
    }

    private void synchronizeChild(CompletedTrainingReportRef child, LocalDateTime now) {
        mapper.registerSource(child);
        if (!mapper.claimSource(child, now)) {
            String status = mapper.syncStatus(child).orElse("MISSING");
            if ("COMPLETED".equals(status)) return;
            if ("REJECTED".equals(status)) {
                throw new EvidenceRejectedException("SIMULATION_CHILD_REJECTED",
                        "a referenced simulation child report was rejected");
            }
            throw new IllegalStateException("simulation child evidence is not ready: " + status);
        }
        extractAndStore(child, now);
        mapper.markCompleted(child);
    }

    private void extractAndStore(CompletedTrainingReportRef ref, LocalDateTime now) {
        CompletedTrainingReport source = sources.findReport(ref).orElseThrow(() ->
                new EvidenceRejectedException("SOURCE_REPORT_MISSING", "completed source report is missing"));
        for (AbilityEvidence evidence : extractor.extract(source, now)) {
            if (mapper.insertEvidenceIgnore(evidence) == 0) {
                metrics.recordEvidenceIdempotentConflict();
            }
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String stableErrorCode(RuntimeException ex) {
        String name = ex.getClass().getSimpleName();
        return name == null || name.isBlank() ? "TRAINING_AGENT_SYNC_FAILED" : name.toUpperCase();
    }
}
