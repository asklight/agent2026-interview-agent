package com.agent2026.interview.trainingagent.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingRecommendationPolicyTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 19, 12, 0);
    private final TrainingRecommendationPolicy policy = new TrainingRecommendationPolicy();

    @Test
    void coldStartUsesThreeMixedJavaQuestions() {
        TrainingRecommendation recommendation = policy.recommend(List.of(), List.of(), NOW);

        assertThat(recommendation.state()).isEqualTo("COLD_START");
        assertThat(recommendation.primary().dimensionCode()).isEqualTo("KNOWLEDGE.JAVA");
        assertThat(recommendation.primary().action())
                .containsEntry("difficulty", "mixed")
                .containsEntry("questionCount", 3)
                .containsEntry("module", "JAVA");
        assertThat(recommendation.primary().evidenceIds()).isEmpty();
    }

    @Test
    void unobservedCoreDimensionIsARecommendationCandidate() {
        AbilityEvidence evidence = evidence(1, 1, AbilityDimension.KNOWLEDGE_REDIS,
                EvidencePolarity.STRENGTH, 3, 1, "Redis 回答完整", NOW);
        List<AbilitySnapshot> snapshots = List.of(
                snapshot(AbilityDimension.KNOWLEDGE_REDIS, AbilityState.STABLE, 3, 0.7, 1, 0, 0, 2, NOW),
                unknown(AbilityDimension.KNOWLEDGE_MYSQL));

        TrainingRecommendation recommendation = policy.recommend(snapshots, List.of(evidence), NOW);

        assertThat(recommendation.primary().dimensionCode()).isEqualTo("KNOWLEDGE.MYSQL");
        assertThat(recommendation.primary().reason()).contains("还没有训练证据");
    }

    @Test
    void coreDimensionWeightWinsWithinSamePriorityGroup() {
        AbilityEvidence mysql = evidence(1, 1, AbilityDimension.KNOWLEDGE_MYSQL,
                EvidencePolarity.GAP, 3, 1, "索引原理不完整", NOW);
        AbilityEvidence spring = evidence(2, 2, AbilityDimension.KNOWLEDGE_SPRING,
                EvidencePolarity.GAP, 4, 0.8, "事务传播不完整", NOW);
        List<AbilitySnapshot> snapshots = List.of(
                snapshot(AbilityDimension.KNOWLEDGE_MYSQL, AbilityState.NEEDS_WORK, -3, 0.6, 0, 1, 0, 1, NOW),
                snapshot(AbilityDimension.KNOWLEDGE_SPRING, AbilityState.NEEDS_WORK, -3.2, 0.6, 0, 1, 0, 1, NOW));

        TrainingRecommendation recommendation = policy.recommend(snapshots, List.of(mysql, spring), NOW);

        assertThat(recommendation.primary().dimensionCode()).isEqualTo("KNOWLEDGE.MYSQL");
    }

    @Test
    void twoRecentTrainingsApplyFatigueToThatTrainingType() {
        AbilityEvidence algorithm = evidence(1, 1, AbilityDimension.ALGORITHM_COMPLEXITY,
                EvidencePolarity.GAP, 3, 1, "遗漏空间复杂度", NOW);
        AbilityEvidence project = evidence(2, 2, AbilityDimension.PROJECT_PRINCIPLE,
                EvidencePolarity.GAP, 3, 1, "原理解释偏浅", NOW);
        List<AbilitySnapshot> snapshots = List.of(
                snapshot(AbilityDimension.ALGORITHM_COMPLEXITY, AbilityState.NEEDS_WORK, -3, 0.6, 0, 1, 0, 1, NOW),
                snapshot(AbilityDimension.PROJECT_PRINCIPLE, AbilityState.NEEDS_WORK, -3, 0.6, 0, 1, 0, 1, NOW));

        TrainingRecommendation recommendation = policy.recommend(snapshots, List.of(algorithm, project),
                new TrainingHistorySignal(List.of("ALGORITHM", "ALGORITHM")), NOW);

        assertThat(recommendation.primary().trainingType()).isEqualTo("PROJECT_DEEP_DIVE");
    }

    @Test
    void highRiskIsNotPenalizedByFatigue() {
        AbilityEvidence algorithm = evidence(1, 1, AbilityDimension.ALGORITHM_COMPLEXITY,
                EvidencePolarity.RISK, 4, 0.8, "复杂度结论错误", NOW);
        AbilityEvidence project = evidence(2, 2, AbilityDimension.PROJECT_PRINCIPLE,
                EvidencePolarity.RISK, 4, 0.8, "核心机制解释错误", NOW);
        List<AbilitySnapshot> snapshots = List.of(
                snapshot(AbilityDimension.ALGORITHM_COMPLEXITY, AbilityState.NEEDS_WORK, -4, 0.8, 0, 0, 1, 1, NOW),
                snapshot(AbilityDimension.PROJECT_PRINCIPLE, AbilityState.NEEDS_WORK, -4, 0.8, 0, 0, 1, 1, NOW));

        TrainingRecommendation withoutHistory = policy.recommend(snapshots, List.of(algorithm, project), NOW);
        TrainingRecommendation withHistory = policy.recommend(snapshots, List.of(algorithm, project),
                new TrainingHistorySignal(List.of("ALGORITHM", "ALGORITHM")), NOW);

        assertThat(withHistory.primary()).isEqualTo(withoutHistory.primary());
    }

    @Test
    void primaryAndAlternativesNeverRepeatTrainingType() {
        List<AbilityEvidence> evidence = List.of(
                evidence(1, 1, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.GAP, 3, 1, "JVM 不完整", NOW),
                evidence(2, 2, AbilityDimension.PROJECT_PRINCIPLE, EvidencePolarity.GAP, 3, 1, "原理不完整", NOW),
                evidence(3, 3, AbilityDimension.PROJECT_TRADEOFF, EvidencePolarity.GAP, 3, 1, "取舍不完整", NOW),
                evidence(4, 4, AbilityDimension.ALGORITHM_COMPLEXITY, EvidencePolarity.GAP, 3, 1, "复杂度不完整", NOW));
        List<AbilitySnapshot> snapshots = List.of(
                needsWork(AbilityDimension.KNOWLEDGE_JAVA, NOW),
                needsWork(AbilityDimension.PROJECT_PRINCIPLE, NOW),
                needsWork(AbilityDimension.PROJECT_TRADEOFF, NOW),
                needsWork(AbilityDimension.ALGORITHM_COMPLEXITY, NOW));

        TrainingRecommendation recommendation = policy.recommend(snapshots, evidence, NOW);
        List<String> types = new ArrayList<>();
        types.add(recommendation.primary().trainingType());
        recommendation.alternatives().forEach(item -> types.add(item.trainingType()));

        assertThat(types).hasSize(3).doesNotHaveDuplicates();
    }

    @Test
    void strongDimensionIsUsedOnlyAsFallbackAndCitesItsEvidence() {
        AbilityEvidence evidence = evidence(91, 1, AbilityDimension.KNOWLEDGE_JAVA,
                EvidencePolarity.STRENGTH, 4, 0.9, "并发原理解释清楚", NOW);
        AbilitySnapshot strong = snapshot(AbilityDimension.KNOWLEDGE_JAVA, AbilityState.STRONG,
                6, 0.9, 3, 0, 0, 3, NOW);

        TrainingRecommendation recommendation = policy.recommend(List.of(strong), List.of(evidence), NOW);

        assertThat(recommendation.primary().dimensionCode()).isEqualTo("KNOWLEDGE.JAVA");
        assertThat(recommendation.primary().reason()).contains("并发原理解释清楚").contains("保持训练");
        assertThat(recommendation.primary().evidenceIds()).containsExactly(91L);
    }

    @Test
    void repeatedGapsOutrankNeedsWorkAndReasonCitesEvidence() {
        AbilityEvidence firstGap = evidence(11, 1, AbilityDimension.ALGORITHM_COMPLEXITY,
                EvidencePolarity.GAP, 2, 0.8, "第一次遗漏空间复杂度", NOW.minusDays(2));
        AbilityEvidence secondGap = evidence(12, 2, AbilityDimension.ALGORITHM_COMPLEXITY,
                EvidencePolarity.GAP, 2, 0.8, "再次遗漏空间复杂度", NOW.minusDays(1));
        AbilityEvidence needsWork = evidence(13, 3, AbilityDimension.PROJECT_PRINCIPLE,
                EvidencePolarity.GAP, 5, 1, "核心原理解释错误", NOW);
        List<AbilitySnapshot> snapshots = List.of(
                snapshot(AbilityDimension.ALGORITHM_COMPLEXITY, AbilityState.DEVELOPING,
                        -2, 0.6, 0, 2, 0, 2, NOW.minusDays(1)),
                snapshot(AbilityDimension.PROJECT_PRINCIPLE, AbilityState.NEEDS_WORK,
                        -5, 0.5, 0, 1, 0, 1, NOW));

        TrainingRecommendation recommendation = policy.recommend(snapshots,
                List.of(firstGap, secondGap, needsWork), NOW);

        assertThat(recommendation.primary().dimensionCode()).isEqualTo("ALGORITHM.COMPLEXITY");
        assertThat(recommendation.primary().reason()).contains("再次遗漏空间复杂度");
        assertThat(recommendation.primary().evidenceIds()).contains(12L);
    }

    @Test
    void recommendationIsDeterministicRegardlessOfInputOrder() {
        AbilityEvidence algorithm = evidence(1, 1, AbilityDimension.ALGORITHM_COMPLEXITY,
                EvidencePolarity.GAP, 3, 1, "遗漏复杂度", NOW);
        AbilityEvidence project = evidence(2, 2, AbilityDimension.PROJECT_PRINCIPLE,
                EvidencePolarity.GAP, 3, 1, "原理偏浅", NOW);
        List<AbilitySnapshot> snapshots = new ArrayList<>(List.of(
                needsWork(AbilityDimension.ALGORITHM_COMPLEXITY, NOW),
                needsWork(AbilityDimension.PROJECT_PRINCIPLE, NOW)));
        List<AbilityEvidence> evidence = new ArrayList<>(List.of(algorithm, project));
        TrainingRecommendation expected = policy.recommend(snapshots, evidence, NOW);

        Collections.reverse(snapshots);
        Collections.reverse(evidence);

        assertThat(policy.recommend(snapshots, evidence, NOW)).isEqualTo(expected);
    }

    private AbilitySnapshot unknown(AbilityDimension dimension) {
        return snapshot(dimension, AbilityState.UNKNOWN, 0, 0, 0, 0, 0, 0, null);
    }

    private AbilitySnapshot needsWork(AbilityDimension dimension, LocalDateTime observedAt) {
        return snapshot(dimension, AbilityState.NEEDS_WORK, -3, 0.6, 0, 1, 0, 1, observedAt);
    }

    private AbilitySnapshot snapshot(AbilityDimension dimension, AbilityState state, double value, double confidence,
                                     int strengths, int gaps, int risks, int sessions, LocalDateTime observedAt) {
        return new AbilitySnapshot(dimension, state, value, confidence, strengths, gaps, risks, sessions, observedAt);
    }

    private AbilityEvidence evidence(long id, long sessionId, AbilityDimension dimension, EvidencePolarity polarity,
                                     int severity, double confidence, String text, LocalDateTime observedAt) {
        return new AbilityEvidence(id, 1L, dimension.sourceType(), sessionId, id, 1, "e-" + id,
                dimension, polarity, severity, confidence, text, id, id, Map.of(), observedAt);
    }
}
