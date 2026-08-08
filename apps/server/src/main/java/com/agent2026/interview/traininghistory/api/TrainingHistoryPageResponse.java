package com.agent2026.interview.traininghistory.api;

import java.util.List;

public record TrainingHistoryPageResponse(List<TrainingHistoryItemResponse> items, long total,
                                          int page, int pageSize) {
    public TrainingHistoryPageResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
