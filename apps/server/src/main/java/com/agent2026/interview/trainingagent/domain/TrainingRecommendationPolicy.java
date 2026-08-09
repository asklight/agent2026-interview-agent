package com.agent2026.interview.trainingagent.domain;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TrainingRecommendationPolicy {
    public static final String POLICY_VERSION = "v1";

    public TrainingRecommendation recommend(List<AbilitySnapshot> snapshots, List<AbilityEvidence> evidence,
                                            LocalDateTime now) {
        if (evidence.isEmpty()) {
            return new TrainingRecommendation("COLD_START", new TrainingRecommendation.Item(
                    "KNOWLEDGE", AbilityDimension.KNOWLEDGE_JAVA.code(), "Java 核心 · 3 题快速校准",
                    "完成首次校准后，系统才能依据真实表现安排训练。", 10,
                    action(AbilityDimension.KNOWLEDGE_JAVA, "medium", 3), List.of()), List.of());
        }
        List<Candidate> candidates = snapshots.stream()
                .filter(snapshot -> snapshot.state() != AbilityState.STRONG)
                // 有证据后只在被观测过的维度之间做推荐，避免把“从未练过”的几十个维度当成缺口。
                .filter(snapshot -> evidence.stream().anyMatch(item -> item.dimension() == snapshot.dimension()))
                .map(snapshot -> candidate(snapshot, evidence, now))
                .sorted(Comparator.comparingInt(Candidate::group).thenComparing(Comparator.comparingDouble(Candidate::score).reversed())
                        .thenComparing(Candidate::lastObserved, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Candidate primary = candidates.isEmpty() ? strongestFallback(snapshots, evidence, now) : candidates.get(0);
        List<TrainingRecommendation.Item> alternatives = new ArrayList<>();
        for (int i = 1; i < candidates.size() && alternatives.size() < 2; i++) {
            Candidate candidate = candidates.get(i);
            if (!candidate.item().trainingType().equals(primary.item().trainingType())) alternatives.add(candidate.item());
        }
        return new TrainingRecommendation("READY", primary.item(), alternatives);
    }

    private Candidate candidate(AbilitySnapshot snapshot, List<AbilityEvidence> evidence, LocalDateTime now) {
        List<AbilityEvidence> related = evidence.stream().filter(item -> item.dimension() == snapshot.dimension()).toList();
        AbilityEvidence latest = related.stream().max(Comparator.comparing(AbilityEvidence::observedAt)).orElse(null);
        long negativeSessions = related.stream().filter(item -> item.polarity() != EvidencePolarity.STRENGTH)
                .map(AbilityEvidence::sourceSessionId).distinct().count();
        boolean risk = related.stream().anyMatch(item -> item.polarity() == EvidencePolarity.RISK && item.severity() >= 4
                && item.confidence() >= 0.7 && Duration.between(item.observedAt(), now).toDays() <= 30);
        int group = risk ? 0 : negativeSessions >= 2 ? 1 : snapshot.state() == AbilityState.NEEDS_WORK ? 2
                : snapshot.state() == AbilityState.UNKNOWN ? 3 : isStale(snapshot, now) ? 4 : 5;
        double recurrence = 1 + Math.min(0.5, Math.max(0, negativeSessions - 1) * 0.15);
        // 一期先保持确定性；近期连续训练的疲劳惩罚会在训练历史接入后补充。
        double fatigue = 1.0;
        double score = Math.max(0.2, Math.abs(snapshot.internalValue()) + snapshot.confidence()) * recurrence * fatigue;
        return new Candidate(group, score, latest == null ? null : latest.observedAt(), item(snapshot, related, group, risk, now));
    }

    private Candidate strongestFallback(List<AbilitySnapshot> snapshots, List<AbilityEvidence> evidence, LocalDateTime now) {
        AbilitySnapshot snapshot = snapshots.stream().filter(item -> item.state() != AbilityState.UNKNOWN)
                .max(Comparator.comparingDouble(AbilitySnapshot::internalValue)).orElse(null);
        if (snapshot == null) snapshot = new AbilitySnapshot(AbilityDimension.KNOWLEDGE_JAVA, AbilityState.DEVELOPING,
                0, 0, 0, 0, 0, 0, now);
        AbilitySnapshot selected = snapshot;
        List<AbilityEvidence> related = evidence.stream().filter(item -> item.dimension() == selected.dimension()).toList();
        return new Candidate(5, 0, selected.lastObservedAt(), item(selected, related, 5, false, now));
    }

    private TrainingRecommendation.Item item(AbilitySnapshot snapshot, List<AbilityEvidence> related, int group, boolean risk,
                                             LocalDateTime now) {
        AbilityDimension dimension = snapshot.dimension();
        String type = dimension.sourceType();
        if ("GENERAL".equals(type)) {
            type = related.stream().max(Comparator.comparing(AbilityEvidence::observedAt)).map(AbilityEvidence::sourceType).orElse("KNOWLEDGE");
        }
        String reason;
        if (snapshot.state() == AbilityState.UNKNOWN) reason = "这个能力维度还没有训练证据，先完成一次基础校准。";
        else if (risk) reason = "最近训练出现了需要优先处理的风险信号，建议先针对这一维度复盘。";
        else if (snapshot.distinctSessionCount() >= 2 && snapshot.gapCount() + snapshot.riskCount() > 0)
            reason = "最近 " + snapshot.distinctSessionCount() + " 次训练都出现了“" + dimension.label() + "”的补强信号。";
        else if (isStale(snapshot, now)) reason = "这个能力维度已经超过 90 天没有重新验证，建议做一次校准。";
        else reason = "根据最近的训练证据，建议继续补强“" + dimension.label() + "”。";
        String title = switch (type) {
            case "PROJECT_DEEP_DIVE" -> "项目深挖 · " + dimension.label();
            case "ALGORITHM" -> "算法口述 · " + dimension.label();
            case "COMPREHENSIVE_SIMULATION" -> "来一场综合模拟";
            default -> dimension.label() + " · 3 题快速校准";
        };
        int minutes = "PROJECT_DEEP_DIVE".equals(type) ? 20 : "ALGORITHM".equals(type) ? 15 : 10;
        return new TrainingRecommendation.Item(type, dimension.code(), title, reason, minutes,
                action(dimension, "medium", 3), related.stream().map(AbilityEvidence::id).filter(id -> id != null).limit(3).toList());
    }

    private Map<String, Object> action(AbilityDimension dimension, String difficulty, int questionCount) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("dimensionCode", dimension.code());
        action.put("difficulty", difficulty);
        action.put("questionCount", questionCount);
        if ("KNOWLEDGE".equals(dimension.sourceType())) action.put("module", dimension.code().substring("KNOWLEDGE.".length()));
        return action;
    }

    private boolean isStale(AbilitySnapshot snapshot, LocalDateTime now) {
        return snapshot.lastObservedAt() != null && Duration.between(snapshot.lastObservedAt(), now).toDays() > 90;
    }

    private record Candidate(int group, double score, LocalDateTime lastObserved, TrainingRecommendation.Item item) {}
}
