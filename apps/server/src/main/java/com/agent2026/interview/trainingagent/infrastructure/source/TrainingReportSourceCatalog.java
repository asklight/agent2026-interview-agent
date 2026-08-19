package com.agent2026.interview.trainingagent.infrastructure.source;

import com.agent2026.interview.shared.training.CompletedTrainingReport;
import com.agent2026.interview.shared.training.CompletedTrainingReportRef;
import com.agent2026.interview.shared.training.TrainingReportCursor;
import com.agent2026.interview.shared.training.TrainingReportSourceQuery;
import com.agent2026.interview.shared.training.TrainingSourceTypes;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TrainingReportSourceCatalog {
    private final Map<String, TrainingReportSourceQuery> sources;

    public TrainingReportSourceCatalog(List<TrainingReportSourceQuery> queries) {
        Map<String, TrainingReportSourceQuery> indexed = new LinkedHashMap<>();
        for (TrainingReportSourceQuery query : queries) {
            for (String sourceType : query.sourceTypes()) {
                TrainingReportSourceQuery duplicate = indexed.putIfAbsent(sourceType, query);
                if (duplicate != null) {
                    throw new IllegalStateException("duplicate training report source: " + sourceType);
                }
            }
        }
        for (String sourceType : TrainingSourceTypes.ORDERED) {
            if (!indexed.containsKey(sourceType)) {
                throw new IllegalStateException("missing training report source: " + sourceType);
            }
        }
        this.sources = Map.copyOf(indexed);
    }

    public List<String> sourceTypes() {
        return TrainingSourceTypes.ORDERED;
    }

    public List<CompletedTrainingReportRef> scan(String sourceType, TrainingReportCursor cursor, int limit) {
        return source(sourceType).scanCompleted(sourceType, null, cursor, limit);
    }

    public List<CompletedTrainingReportRef> recent(Long userId, int perSourceLimit) {
        List<CompletedTrainingReportRef> result = new ArrayList<>();
        for (String sourceType : sourceTypes()) {
            result.addAll(source(sourceType).scanCompleted(sourceType, userId, null, perSourceLimit));
        }
        result.sort(Comparator.comparing(CompletedTrainingReportRef::completedAt).reversed()
                .thenComparing(CompletedTrainingReportRef::sourceType)
                .thenComparing(CompletedTrainingReportRef::sourceSessionId, Comparator.reverseOrder()));
        return List.copyOf(result);
    }

    public Optional<CompletedTrainingReport> findReport(CompletedTrainingReportRef ref) {
        return source(ref.sourceType()).findReport(ref);
    }

    public List<CompletedTrainingReportRef> findCompletedChildren(Long userId, Long simulationSessionId) {
        return source(TrainingSourceTypes.COMPREHENSIVE_SIMULATION)
                .findCompletedChildren(userId, simulationSessionId);
    }

    public List<String> recentTrainingTypes(Long userId, int limit) {
        return recent(userId, Math.max(1, limit)).stream()
                .limit(Math.max(1, limit))
                .map(CompletedTrainingReportRef::sourceType)
                .toList();
    }

    private TrainingReportSourceQuery source(String sourceType) {
        TrainingReportSourceQuery source = sources.get(sourceType);
        if (source == null) throw new IllegalArgumentException("unsupported training source type: " + sourceType);
        return source;
    }
}
