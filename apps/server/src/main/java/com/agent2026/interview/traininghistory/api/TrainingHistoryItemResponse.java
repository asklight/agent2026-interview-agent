package com.agent2026.interview.traininghistory.api;

import com.agent2026.interview.traininghistory.persistence.TrainingHistoryEntity;

import java.time.LocalDateTime;

public record TrainingHistoryItemResponse(Long id, String trainingType, Long sourceSessionId, String status,
                                          String title, String summary, LocalDateTime startedAt,
                                          LocalDateTime finishedAt) {
    public static TrainingHistoryItemResponse from(TrainingHistoryEntity entity) {
        return new TrainingHistoryItemResponse(entity.getId(), entity.getTrainingType(),
                entity.getSourceSessionId(), entity.getStatus(), entity.getTitle(), entity.getSummary(),
                entity.getStartedAt(), entity.getFinishedAt());
    }
}
