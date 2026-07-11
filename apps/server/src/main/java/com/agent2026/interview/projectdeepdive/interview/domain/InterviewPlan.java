package com.agent2026.interview.projectdeepdive.interview.domain;

import java.util.List;

public record InterviewPlan(Long id, Long sessionId, String projectProfileSnapshotJson,
                            List<PlannedProbe> plannedProbes, int templateVersion, String status) {
    public InterviewPlan {
        plannedProbes = plannedProbes == null ? List.of() : List.copyOf(plannedProbes);
    }
}
