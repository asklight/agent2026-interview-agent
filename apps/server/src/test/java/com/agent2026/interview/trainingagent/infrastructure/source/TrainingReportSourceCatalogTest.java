package com.agent2026.interview.trainingagent.infrastructure.source;

import com.agent2026.interview.shared.training.CompletedTrainingReport;
import com.agent2026.interview.shared.training.CompletedTrainingReportRef;
import com.agent2026.interview.shared.training.TrainingReportSourceQuery;
import com.agent2026.interview.shared.training.TrainingSourceTypes;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainingReportSourceCatalogTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 19, 10, 0);

    @Test
    void routesSimulationChildrenBackToTheirOwningReportSource() {
        TrainingReportSourceQuery interviews = source(
                Set.of(TrainingSourceTypes.KNOWLEDGE, TrainingSourceTypes.PROJECT_DEEP_DIVE));
        TrainingReportSourceQuery algorithms = source(Set.of(TrainingSourceTypes.ALGORITHM));
        TrainingReportSourceQuery simulations = source(Set.of(TrainingSourceTypes.COMPREHENSIVE_SIMULATION));
        TrainingReportSourceCatalog catalog = new TrainingReportSourceCatalog(
                List.of(interviews, algorithms, simulations));
        CompletedTrainingReportRef child = ref(TrainingSourceTypes.ALGORITHM, 31L, NOW);
        CompletedTrainingReport report = new CompletedTrainingReport(TrainingSourceTypes.ALGORITHM,
                7L, 31L, 91L, 1, null, "medium", "array", null,
                "{}", null, null, null, NOW);
        when(algorithms.findReport(child)).thenReturn(Optional.of(report));

        assertThat(catalog.findReport(child)).contains(report);

        verify(algorithms).findReport(child);
    }

    @Test
    void recentTrainingTypesAreGloballyOrderedAcrossSources() {
        TrainingReportSourceQuery interviews = source(
                Set.of(TrainingSourceTypes.KNOWLEDGE, TrainingSourceTypes.PROJECT_DEEP_DIVE));
        TrainingReportSourceQuery algorithms = source(Set.of(TrainingSourceTypes.ALGORITHM));
        TrainingReportSourceQuery simulations = source(Set.of(TrainingSourceTypes.COMPREHENSIVE_SIMULATION));
        TrainingReportSourceCatalog catalog = new TrainingReportSourceCatalog(
                List.of(interviews, algorithms, simulations));
        when(interviews.scanCompleted(TrainingSourceTypes.KNOWLEDGE, 7L, null, 2))
                .thenReturn(List.of(ref(TrainingSourceTypes.KNOWLEDGE, 11L, NOW.minusMinutes(3))));
        when(interviews.scanCompleted(TrainingSourceTypes.PROJECT_DEEP_DIVE, 7L, null, 2))
                .thenReturn(List.of(ref(TrainingSourceTypes.PROJECT_DEEP_DIVE, 12L, NOW)));
        when(algorithms.scanCompleted(TrainingSourceTypes.ALGORITHM, 7L, null, 2))
                .thenReturn(List.of(ref(TrainingSourceTypes.ALGORITHM, 13L, NOW.minusMinutes(1))));
        when(simulations.scanCompleted(TrainingSourceTypes.COMPREHENSIVE_SIMULATION, 7L, null, 2))
                .thenReturn(List.of());

        assertThat(catalog.recentTrainingTypes(7L, 2))
                .containsExactly(TrainingSourceTypes.PROJECT_DEEP_DIVE, TrainingSourceTypes.ALGORITHM);
    }

    private TrainingReportSourceQuery source(Set<String> types) {
        TrainingReportSourceQuery source = mock(TrainingReportSourceQuery.class);
        when(source.sourceTypes()).thenReturn(types);
        return source;
    }

    private CompletedTrainingReportRef ref(String sourceType, Long sessionId, LocalDateTime completedAt) {
        return new CompletedTrainingReportRef(sourceType, 7L, sessionId, 1, completedAt);
    }
}
