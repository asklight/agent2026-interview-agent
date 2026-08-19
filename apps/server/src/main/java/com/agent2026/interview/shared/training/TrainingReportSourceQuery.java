package com.agent2026.interview.shared.training;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Public read-only report facts exposed by a training source module. */
public interface TrainingReportSourceQuery {
    Set<String> sourceTypes();

    List<CompletedTrainingReportRef> scanCompleted(String sourceType, Long userId,
                                                   TrainingReportCursor beforeExclusive, int limit);

    Optional<CompletedTrainingReport> findReport(CompletedTrainingReportRef ref);

    default List<CompletedTrainingReportRef> findCompletedChildren(Long userId, Long sourceSessionId) {
        return List.of();
    }
}
