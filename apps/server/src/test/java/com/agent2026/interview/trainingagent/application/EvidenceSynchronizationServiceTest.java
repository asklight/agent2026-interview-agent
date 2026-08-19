package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.trainingagent.domain.AbilityDimension;
import com.agent2026.interview.trainingagent.domain.AbilityEvidence;
import com.agent2026.interview.trainingagent.domain.EvidencePolarity;
import com.agent2026.interview.shared.training.CompletedTrainingReport;
import com.agent2026.interview.shared.training.CompletedTrainingReportRef;
import com.agent2026.interview.shared.training.TrainingReportCursor;
import com.agent2026.interview.shared.training.TrainingSourceTypes;
import com.agent2026.interview.trainingagent.infrastructure.persistence.TrainingAgentMapper;
import com.agent2026.interview.trainingagent.infrastructure.source.TrainingEvidenceExtractor;
import com.agent2026.interview.trainingagent.infrastructure.source.TrainingReportSourceCatalog;
import com.agent2026.interview.trainingagent.infrastructure.metrics.TrainingAgentMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EvidenceSynchronizationServiceTest {
    private static final Instant INSTANT = Instant.parse("2026-08-19T02:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZoneOffset.UTC);

    private final TrainingAgentMapper mapper = mock(TrainingAgentMapper.class);
    private final TrainingReportSourceCatalog sources = mock(TrainingReportSourceCatalog.class);
    private final TrainingEvidenceExtractor extractor = mock(TrainingEvidenceExtractor.class);
    private final TrainingAgentProjectionService projections = mock(TrainingAgentProjectionService.class);
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private final EvidenceSynchronizationService service = new EvidenceSynchronizationService(
            mapper, sources, extractor, projections, transactions, Clock.fixed(INSTANT, ZoneOffset.UTC),
            new TrainingAgentMetrics(new SimpleMeterRegistry()));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void executeTransactionCallbacks() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any(Consumer.class));
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        }).when(transactions).execute(any(TransactionCallback.class));
    }

    @Test
    void repeatedSynchronizationOnlyExtractsAndProjectsForTheClaimedAttempt() {
        CompletedTrainingReportRef ref = ref("KNOWLEDGE", 11L);
        CompletedTrainingReport source = source("KNOWLEDGE", 11L);
        AbilityEvidence evidence = evidence(11L);
        when(mapper.claimSource(ref, NOW)).thenReturn(true, false);
        when(sources.findReport(ref)).thenReturn(Optional.of(source));
        when(extractor.extract(source, NOW)).thenReturn(List.of(evidence));

        assertThat(service.registerAndSynchronize(ref)).isTrue();
        assertThat(service.registerAndSynchronize(ref)).isFalse();

        verify(mapper, times(2)).registerSource(ref);
        verify(mapper, times(2)).claimSource(ref, NOW);
        verify(mapper).insertEvidenceIgnore(evidence);
        verify(projections).recompute(7L, NOW);
        verify(mapper).markCompleted(ref);
        verify(extractor).extract(source, NOW);
    }

    @Test
    void stableSourceProblemIsRejectedWithoutSchedulingRetry() {
        CompletedTrainingReportRef ref = ref("PROJECT_DEEP_DIVE", 21L);
        when(mapper.claimSource(ref, NOW)).thenReturn(true);
        when(sources.findReport(ref)).thenReturn(Optional.empty());

        assertThat(service.registerAndSynchronize(ref)).isFalse();

        verify(mapper).markRejected(ref, "SOURCE_REPORT_MISSING");
        verify(mapper, never()).markFailed(eq(ref), any(), any());
        verify(projections, never()).recompute(any(), any());
        verify(mapper, never()).markCompleted(ref);
    }

    @Test
    void transientFailureUsesAttemptBasedBackoffInsteadOfRejectingSource() {
        CompletedTrainingReportRef ref = ref("ALGORITHM", 31L);
        when(mapper.claimSource(ref, NOW)).thenReturn(true);
        when(sources.findReport(ref)).thenThrow(new IllegalStateException("database temporarily unavailable"));
        when(mapper.attemptCount(ref)).thenReturn(3);

        assertThat(service.registerAndSynchronize(ref)).isFalse();

        ArgumentCaptor<LocalDateTime> retryAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).markFailed(eq(ref), retryAt.capture(), eq("ILLEGALSTATEEXCEPTION"));
        assertThat(retryAt.getValue()).isEqualTo(NOW.plusSeconds(120));
        verify(mapper, never()).markRejected(eq(ref), any());
        verify(mapper, never()).markCompleted(ref);
    }

    @Test
    void comprehensiveSimulationOnlyExpandsAndSynchronizesChildReports() {
        CompletedTrainingReportRef simulation = ref("COMPREHENSIVE_SIMULATION", 51L);
        CompletedTrainingReportRef project = ref("PROJECT_DEEP_DIVE", 52L);
        CompletedTrainingReportRef algorithm = ref("ALGORITHM", 53L);
        CompletedTrainingReport projectSource = source("PROJECT_DEEP_DIVE", 52L);
        CompletedTrainingReport algorithmSource = source("ALGORITHM", 53L);
        when(mapper.claimSource(any(CompletedTrainingReportRef.class), eq(NOW))).thenReturn(true);
        when(sources.findCompletedChildren(7L, 51L)).thenReturn(List.of(project, algorithm));
        when(sources.findReport(project)).thenReturn(Optional.of(projectSource));
        when(sources.findReport(algorithm)).thenReturn(Optional.of(algorithmSource));
        when(extractor.extract(projectSource, NOW)).thenReturn(List.of());
        when(extractor.extract(algorithmSource, NOW)).thenReturn(List.of());

        assertThat(service.registerAndSynchronize(simulation)).isTrue();

        verify(sources, never()).findReport(simulation);
        verify(sources).findCompletedChildren(7L, 51L);
        verify(mapper).registerSource(project);
        verify(mapper).registerSource(algorithm);
        verify(mapper).markCompleted(project);
        verify(mapper).markCompleted(algorithm);
        verify(mapper).markCompleted(simulation);
        verify(extractor).extract(projectSource, NOW);
        verify(extractor).extract(algorithmSource, NOW);
        verify(projections).recompute(7L, NOW);
    }

    @Test
    void comprehensiveSimulationReusesAnAlreadyCompletedChildWithoutExtractingItAgain() {
        CompletedTrainingReportRef simulation = ref("COMPREHENSIVE_SIMULATION", 61L);
        CompletedTrainingReportRef child = ref("KNOWLEDGE", 62L);
        when(mapper.claimSource(simulation, NOW)).thenReturn(true);
        when(sources.findCompletedChildren(7L, 61L)).thenReturn(List.of(child));
        when(mapper.claimSource(child, NOW)).thenReturn(false);
        when(mapper.syncStatus(child)).thenReturn(Optional.of("COMPLETED"));

        assertThat(service.registerAndSynchronize(simulation)).isTrue();

        verify(mapper).syncStatus(child);
        verify(sources, never()).findReport(child);
        verify(extractor, never()).extract(any(), any());
        verify(mapper, never()).markCompleted(child);
        verify(mapper).markCompleted(simulation);
        verify(projections).recompute(7L, NOW);
    }

    @Test
    void comprehensiveSimulationRejectsWhenAReferencedChildWasRejected() {
        CompletedTrainingReportRef simulation = ref("COMPREHENSIVE_SIMULATION", 71L);
        CompletedTrainingReportRef child = ref("ALGORITHM", 72L);
        when(mapper.claimSource(simulation, NOW)).thenReturn(true);
        when(sources.findCompletedChildren(7L, 71L)).thenReturn(List.of(child));
        when(mapper.claimSource(child, NOW)).thenReturn(false);
        when(mapper.syncStatus(child)).thenReturn(Optional.of("REJECTED"));

        assertThat(service.registerAndSynchronize(simulation)).isFalse();

        verify(mapper).markRejected(simulation, "SIMULATION_CHILD_REJECTED");
        verify(mapper, never()).markFailed(eq(simulation), any(), any());
        verify(projections, never()).recompute(any(), any());
    }

    @Test
    void lightweightBackfillNeverProcessesMoreThanTenSources() {
        List<CompletedTrainingReportRef> retryable = java.util.stream.LongStream.rangeClosed(1, 10)
                .mapToObj(id -> ref("KNOWLEDGE", id))
                .toList();
        when(sources.recent(7L, 30)).thenReturn(List.of());
        when(mapper.findRetryableSources(7L, NOW, 10)).thenReturn(retryable);

        assertThat(service.lightweightBackfill(7L, 100)).isZero();

        verify(sources).recent(7L, 30);
        verify(mapper).findRetryableSources(7L, NOW, 10);
        verify(mapper, times(10)).registerSource(any(CompletedTrainingReportRef.class));
        verify(mapper, times(10)).claimSource(any(CompletedTrainingReportRef.class), eq(NOW));
    }

    @Test
    void scheduledDiscoveryAdvancesEverySourceCursorAfterRegistration() {
        List<String> sourceTypes = TrainingSourceTypes.ORDERED;
        when(sources.sourceTypes()).thenReturn(sourceTypes);
        when(mapper.registerSource(any(CompletedTrainingReportRef.class))).thenReturn(true);
        for (int index = 0; index < sourceTypes.size(); index++) {
            String type = sourceTypes.get(index);
            when(sources.scan(eq(type), eq(null), anyInt())).thenReturn(List.of(ref(type, 80L + index)));
        }
        when(mapper.findRetryableSources(null, NOW, 100)).thenReturn(List.of());

        assertThat(service.scheduledBackfill(100)).isZero();

        for (int index = 0; index < sourceTypes.size(); index++) {
            String type = sourceTypes.get(index);
            CompletedTrainingReportRef item = ref(type, 80L + index);
            verify(mapper).advanceScanCursor(type,
                    new TrainingReportCursor(item.completedAt(), item.sourceSessionId()));
        }
    }

    @Test
    void scheduledDiscoveryResetsACompletedSourceScanCycleOnEmptyPage() {
        when(sources.sourceTypes()).thenReturn(List.of(TrainingSourceTypes.KNOWLEDGE));
        TrainingReportCursor cursor = new TrainingReportCursor(NOW.minusDays(1), 9L);
        when(mapper.findScanCursor(TrainingSourceTypes.KNOWLEDGE)).thenReturn(cursor);
        when(sources.scan(TrainingSourceTypes.KNOWLEDGE, cursor, 10)).thenReturn(List.of());
        when(mapper.findRetryableSources(null, NOW, 10)).thenReturn(List.of());

        service.scheduledBackfill(10);

        verify(mapper).resetScanCursor(TrainingSourceTypes.KNOWLEDGE);
        verify(mapper, never()).advanceScanCursor(eq(TrainingSourceTypes.KNOWLEDGE), any());
    }

    private CompletedTrainingReportRef ref(String sourceType, Long sessionId) {
        return new CompletedTrainingReportRef(sourceType, 7L, sessionId, 1, NOW.minusMinutes(5));
    }

    private CompletedTrainingReport source(String sourceType, Long sessionId) {
        return new CompletedTrainingReport(sourceType, 7L, sessionId, sessionId + 100, 1,
                sourceType.equals("KNOWLEDGE") ? "Java" : null, "mixed", "", 9L,
                null, "clear explanation", null, null, NOW.minusMinutes(5));
    }

    private AbilityEvidence evidence(Long sessionId) {
        return new AbilityEvidence(null, 7L, "KNOWLEDGE", sessionId, sessionId + 100, 1,
                "key-" + sessionId, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH,
                3, 0.8, "clear explanation", null, null, Map.of(), NOW.minusMinutes(5));
    }
}
