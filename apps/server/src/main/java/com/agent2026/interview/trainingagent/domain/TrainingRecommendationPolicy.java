package com.agent2026.interview.trainingagent.domain;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TrainingRecommendationPolicy {
    public static final String POLICY_VERSION = "v2";
    private static final double CORE_DIMENSION_WEIGHT = 1.15;
    private static final double FATIGUE_FACTOR = 0.75;

    public TrainingRecommendation recommend(List<AbilitySnapshot> snapshots, List<AbilityEvidence> evidence,
                                             LocalDateTime now) {
        return recommend(snapshots, evidence, TrainingHistorySignal.none(), now);
    }

    public TrainingRecommendation recommend(List<AbilitySnapshot> snapshots, List<AbilityEvidence> evidence,
                                             TrainingHistorySignal history, LocalDateTime now) {
        if (evidence.isEmpty()) {
            return coldStart();
        }

        List<Candidate> candidates = snapshots.stream()
                .map(snapshot -> candidate(snapshot, evidence, history, now))
                .flatMap(java.util.Optional::stream)
                .sorted(candidateComparator())
                .toList();

        Candidate primary = candidates.isEmpty()
                ? strongestFallback(snapshots, evidence, history, now)
                : candidates.get(0);
        Set<String> usedTrainingTypes = new HashSet<>();
        usedTrainingTypes.add(primary.item().trainingType());
        List<TrainingRecommendation.Item> alternatives = new ArrayList<>();
        for (Candidate candidate : candidates) {
            String trainingType = candidate.item().trainingType();
            if (alternatives.size() == 2) break;
            if (usedTrainingTypes.add(trainingType)) alternatives.add(candidate.item());
        }
        return new TrainingRecommendation("READY", primary.item(), alternatives);
    }

    private TrainingRecommendation coldStart() {
        AbilityDimension dimension = AbilityDimension.KNOWLEDGE_JAVA;
        return new TrainingRecommendation("COLD_START", new TrainingRecommendation.Item(
                "KNOWLEDGE", dimension.code(), "Java 核心 · 3 题快速校准",
                "完成首次校准后，系统才能依据真实表现安排训练。", 10,
                action(dimension, "mixed", 3, null), List.of()), List.of());
    }

    private java.util.Optional<Candidate> candidate(AbilitySnapshot snapshot, List<AbilityEvidence> evidence,
                                                     TrainingHistorySignal history, LocalDateTime now) {
        List<AbilityEvidence> related = sortedEvidence(evidence.stream()
                .filter(item -> item.dimension() == snapshot.dimension()).toList(), now);
        boolean recentHighRisk = related.stream().anyMatch(item -> isRecentHighRisk(item, now));
        long gapSessions = related.stream().filter(item -> item.polarity() == EvidencePolarity.GAP)
                .map(AbilityEvidence::sourceSessionId).distinct().count();

        int group;
        if (recentHighRisk) group = 0;
        else if (gapSessions >= 2) group = 1;
        else if (snapshot.state() == AbilityState.NEEDS_WORK) group = 2;
        else if (snapshot.state() == AbilityState.UNKNOWN && snapshot.dimension().core()) group = 3;
        else if (isStale(snapshot, now)) group = 4;
        else if (snapshot.state() == AbilityState.STABLE) group = 5;
        else return java.util.Optional.empty();

        List<AbilityEvidence> supporting = supportingEvidence(related, group, now);
        String trainingType = resolveTrainingType(snapshot.dimension(), supporting);
        AbilityEvidence supportingEvidence = supporting.isEmpty() ? null : supporting.get(0);
        double baseScore = supportingEvidence == null ? 1.0 : evidenceScore(supportingEvidence, now);
        double recurrence = 1 + Math.min(0.5, 0.15 * Math.max(0, snapshot.distinctSessionCount() - 1));
        double coreWeight = snapshot.dimension().core() ? CORE_DIMENSION_WEIGHT : 1.0;
        double fatigue = recentHighRisk || !history.isFatigued(trainingType) ? 1.0 : FATIGUE_FACTOR;
        double score = baseScore * recurrence * coreWeight * fatigue;
        LocalDateTime lastObserved = supportingEvidence == null ? null : supportingEvidence.observedAt();
        return java.util.Optional.of(new Candidate(group, score, lastObserved,
                item(snapshot, supporting, group, recentHighRisk, trainingType, now)));
    }

    private List<AbilityEvidence> supportingEvidence(List<AbilityEvidence> related, int group, LocalDateTime now) {
        List<AbilityEvidence> focused = switch (group) {
            case 0 -> related.stream().filter(item -> isRecentHighRisk(item, now)).toList();
            case 1 -> related.stream().filter(item -> item.polarity() == EvidencePolarity.GAP).toList();
            case 2 -> related.stream().filter(item -> item.polarity() != EvidencePolarity.STRENGTH).toList();
            default -> related;
        };
        return focused.isEmpty() ? related : sortedEvidence(focused, now);
    }

    private Candidate strongestFallback(List<AbilitySnapshot> snapshots, List<AbilityEvidence> evidence,
                                         TrainingHistorySignal history, LocalDateTime now) {
        AbilitySnapshot snapshot = snapshots.stream()
                .filter(item -> item.state() != AbilityState.UNKNOWN)
                .max(Comparator.comparingDouble(AbilitySnapshot::internalValue)
                        .thenComparing(item -> item.dimension().code(), Comparator.reverseOrder()))
                .orElseThrow(() -> new IllegalStateException("evidence exists without an observed ability snapshot"));
        List<AbilityEvidence> related = sortedEvidence(evidence.stream()
                .filter(item -> item.dimension() == snapshot.dimension()).toList(), now);
        String trainingType = resolveTrainingType(snapshot.dimension(), related);
        double fatigue = history.isFatigued(trainingType) ? FATIGUE_FACTOR : 1.0;
        double baseScore = related.isEmpty() ? 0 : evidenceScore(related.get(0), now);
        return new Candidate(6, baseScore * fatigue, snapshot.lastObservedAt(),
                item(snapshot, related, 6, false, trainingType, now));
    }

    private Comparator<Candidate> candidateComparator() {
        return Comparator.comparingInt(Candidate::group)
                .thenComparing(Comparator.comparingDouble(Candidate::score).reversed())
                .thenComparing(Candidate::lastObserved, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(candidate -> candidate.item().dimensionCode());
    }

    private List<AbilityEvidence> sortedEvidence(List<AbilityEvidence> evidence, LocalDateTime now) {
        return evidence.stream().sorted(Comparator
                        .comparingDouble((AbilityEvidence item) -> evidenceScore(item, now)).reversed()
                        .thenComparing(AbilityEvidence::observedAt, Comparator.reverseOrder())
                        .thenComparing(AbilityEvidence::evidenceKey)
                        .thenComparing(AbilityEvidence::sourceSessionId))
                .toList();
    }

    private double evidenceScore(AbilityEvidence evidence, LocalDateTime now) {
        return evidence.severity() * evidence.confidence() * recencyFactor(evidence.observedAt(), now);
    }

    private double recencyFactor(LocalDateTime observedAt, LocalDateTime now) {
        long age = Math.max(0, Duration.between(observedAt, now).toDays());
        return age <= 7 ? 1.0 : age <= 30 ? 0.85 : age <= 90 ? 0.60 : 0.35;
    }

    private boolean isRecentHighRisk(AbilityEvidence evidence, LocalDateTime now) {
        return evidence.polarity() == EvidencePolarity.RISK
                && evidence.severity() >= 4
                && evidence.confidence() >= 0.7
                && Math.max(0, Duration.between(evidence.observedAt(), now).toDays()) <= 30;
    }

    private TrainingRecommendation.Item item(AbilitySnapshot snapshot, List<AbilityEvidence> related, int group,
                                             boolean risk, String trainingType, LocalDateTime now) {
        AbilityDimension dimension = snapshot.dimension();
        AbilityEvidence supporting = related.isEmpty() ? null : related.get(0);
        String summary = supporting == null || supporting.text() == null || supporting.text().isBlank()
                ? null : supporting.text().trim();
        String reason;
        if (snapshot.state() == AbilityState.UNKNOWN) {
            reason = "“" + dimension.label() + "”还没有训练证据，建议先完成一次基础校准。";
        } else if (risk) {
            reason = evidenceReason(summary, "最近训练出现了“" + dimension.label() + "”的高风险信号，建议优先复盘。");
        } else if (group == 1) {
            reason = evidenceReason(summary, "多个不同场次反复出现“" + dimension.label() + "”的补强信号。");
        } else if (isStale(snapshot, now)) {
            reason = evidenceReason(summary, "“" + dimension.label() + "”已经超过 90 天没有重新验证，建议做一次校准。");
        } else if (snapshot.state() == AbilityState.STRONG) {
            reason = evidenceReason(summary, "“" + dimension.label() + "”表现稳定，可安排一次保持训练。");
        } else if (snapshot.state() == AbilityState.STABLE) {
            reason = evidenceReason(summary, "“" + dimension.label() + "”已经较稳定，建议通过保持训练定期验证。");
        } else {
            reason = evidenceReason(summary, "根据最近证据，建议继续补强“" + dimension.label() + "”。");
        }
        String title = switch (trainingType) {
            case "PROJECT_DEEP_DIVE" -> "项目深挖 · " + dimension.label();
            case "ALGORITHM" -> "算法口述 · " + dimension.label();
            case "COMPREHENSIVE_SIMULATION" -> "来一场综合模拟";
            default -> dimension.label() + " · 3 题快速校准";
        };
        int minutes = "PROJECT_DEEP_DIVE".equals(trainingType) ? 20 : "ALGORITHM".equals(trainingType) ? 15 : 10;
        List<Long> evidenceIds = related.stream().map(AbilityEvidence::id).filter(id -> id != null).limit(3).toList();
        return new TrainingRecommendation.Item(trainingType, dimension.code(), title, reason, minutes,
                action(dimension, "medium", 3, supporting), evidenceIds);
    }

    private String evidenceReason(String evidenceSummary, String conclusion) {
        return evidenceSummary == null ? conclusion : "最近训练记录到：“" + evidenceSummary + "”。" + conclusion;
    }

    private String resolveTrainingType(AbilityDimension dimension, List<AbilityEvidence> related) {
        if (!dimension.general()) return dimension.sourceType();
        return related.stream().findFirst().map(AbilityEvidence::sourceType).orElse("KNOWLEDGE");
    }

    private Map<String, Object> action(AbilityDimension dimension, String difficulty, int questionCount,
                                       AbilityEvidence supporting) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("dimensionCode", dimension.code());
        String observedDifficulty = supporting == null ? null : supporting.metadata().get("difficulty");
        action.put("difficulty", observedDifficulty == null || observedDifficulty.isBlank()
                ? difficulty : observedDifficulty.toLowerCase());
        action.put("questionCount", questionCount);
        if ("KNOWLEDGE".equals(dimension.sourceType())) {
            action.put("module", dimension.code().substring("KNOWLEDGE.".length()));
        }
        if (supporting != null && "PROJECT_DEEP_DIVE".equals(dimension.sourceType())) {
            String profileId = supporting.metadata().get("projectProfileId");
            if (profileId != null && profileId.matches("[1-9][0-9]*")) action.put("profileId", Long.valueOf(profileId));
        }
        if (supporting != null && "ALGORITHM".equals(dimension.sourceType())) {
            String tags = supporting.metadata().get("tags");
            if (tags != null && !tags.isBlank()) {
                String tag = java.util.Arrays.stream(tags.split(","))
                        .map(String::trim).filter(value -> !value.isBlank()).findFirst().orElse(null);
                if (tag != null) action.put("tag", tag);
            }
        }
        return action;
    }

    private boolean isStale(AbilitySnapshot snapshot, LocalDateTime now) {
        return snapshot.lastObservedAt() != null
                && Duration.between(snapshot.lastObservedAt(), now).toDays() > 90;
    }

    private record Candidate(int group, double score, LocalDateTime lastObserved,
                             TrainingRecommendation.Item item) {
    }
}
