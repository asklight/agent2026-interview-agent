package com.agent2026.interview.trainingagent.domain;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AbilityProfileAggregator {
    public static final String POLICY_VERSION = "v1";
    private static final double SESSION_CAP = 5.0;

    public List<AbilitySnapshot> aggregate(List<AbilityEvidence> evidence, LocalDateTime now) {
        Map<AbilityDimension, List<AbilityEvidence>> byDimension = new EnumMap<>(AbilityDimension.class);
        for (AbilityDimension dimension : AbilityDimension.values()) byDimension.put(dimension, new ArrayList<>());
        evidence.forEach(item -> byDimension.get(item.dimension()).add(item));

        List<AbilitySnapshot> snapshots = new ArrayList<>();
        for (AbilityDimension dimension : AbilityDimension.values()) {
            List<AbilityEvidence> items = byDimension.get(dimension);
            if (items.isEmpty()) {
                snapshots.add(new AbilitySnapshot(dimension, AbilityState.UNKNOWN, 0, 0, 0, 0, 0, 0, null));
                continue;
            }
            Map<Long, Double> sessionValues = new HashMap<>();
            double confidenceSum = 0;
            int strength = 0, gap = 0, risk = 0;
            LocalDateTime last = null;
            for (AbilityEvidence item : items) {
                double contribution = contribution(item, now);
                sessionValues.merge(item.sourceSessionId(), contribution, Double::sum);
                confidenceSum += item.confidence();
                if (item.polarity() == EvidencePolarity.STRENGTH) strength++;
                if (item.polarity() == EvidencePolarity.GAP) gap++;
                if (item.polarity() == EvidencePolarity.RISK) risk++;
                if (last == null || item.observedAt().isAfter(last)) last = item.observedAt();
            }
            double net = sessionValues.values().stream().mapToDouble(value -> clamp(value, -SESSION_CAP, SESSION_CAP)).sum();
            int sessions = sessionValues.size();
            double confidence = Math.min(1.0, sessions / 3.0) * (confidenceSum / items.size());
            boolean recentHighRisk = items.stream().anyMatch(item -> item.polarity() == EvidencePolarity.RISK
                    && item.severity() >= 4 && item.confidence() >= 0.7
                    && Duration.between(item.observedAt(), now).toDays() <= 30);
            AbilityState state;
            if (recentHighRisk || net <= -2) state = AbilityState.NEEDS_WORK;
            else if (sessions >= 3 && net >= 5 && confidence >= 0.70 && items.stream().noneMatch(item ->
                    (item.polarity() == EvidencePolarity.GAP || item.polarity() == EvidencePolarity.RISK)
                            && Duration.between(item.observedAt(), now).toDays() <= 30)) state = AbilityState.STRONG;
            else if (sessions >= 2 && net >= 2 && confidence >= 0.55) state = AbilityState.STABLE;
            else state = AbilityState.DEVELOPING;
            snapshots.add(new AbilitySnapshot(dimension, state, round(net), round(confidence), strength, gap, risk, sessions, last));
        }
        return List.copyOf(snapshots);
    }

    private double contribution(AbilityEvidence item, LocalDateTime now) {
        double factor = switch (item.polarity()) {
            case STRENGTH -> 1.0;
            case GAP -> -1.0;
            case RISK -> -1.25;
        };
        long age = Math.max(0, Duration.between(item.observedAt(), now).toDays());
        double recency = age <= 7 ? 1.0 : age <= 30 ? 0.85 : age <= 90 ? 0.60 : 0.35;
        return factor * item.severity() * item.confidence() * recency;
    }

    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private double round(double value) { return Math.round(value * 1000.0) / 1000.0; }
}
