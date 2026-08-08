package com.agent2026.interview.traininghistory.application;

import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.agent2026.interview.traininghistory.api.TrainingHistoryItemResponse;
import com.agent2026.interview.traininghistory.api.TrainingHistoryPageResponse;
import com.agent2026.interview.traininghistory.persistence.TrainingHistoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class TrainingHistoryService {
    private static final Set<String> TYPES = Set.of("KNOWLEDGE", "PROJECT_DEEP_DIVE", "ALGORITHM", "COMPREHENSIVE_SIMULATION");
    private static final Set<String> STATUSES = Set.of("IN_PROGRESS", "FINISHED", "ABANDONED", "CANCELLED");
    private final TrainingHistoryMapper mapper;

    public TrainingHistoryService(TrainingHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public TrainingHistoryPageResponse list(Long userId, String type, String status, int page, int pageSize) {
        String normalizedType = normalize(type, TYPES, "训练类型");
        String normalizedStatus = normalize(status, STATUSES, "训练状态");
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, pageSize));
        mapper.syncInterviews(userId);
        mapper.syncAlgorithms(userId);
        mapper.syncSimulations(userId);
        var items = mapper.page(userId, normalizedType, normalizedStatus, safeSize, (safePage - 1) * safeSize)
                .stream().map(TrainingHistoryItemResponse::from).toList();
        return new TrainingHistoryPageResponse(items,
                mapper.countVisible(userId, normalizedType, normalizedStatus), safePage, safeSize);
    }

    public void hide(Long userId, Long id) {
        if (mapper.hide(id, userId) != 1) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private String normalize(String value, Set<String> allowed, String label) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, label + "不合法");
        }
        return normalized;
    }
}
