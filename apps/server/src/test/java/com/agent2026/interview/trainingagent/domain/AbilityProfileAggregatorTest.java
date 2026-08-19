package com.agent2026.interview.trainingagent.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AbilityProfileAggregatorTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 19, 12, 0);
    private final AbilityProfileAggregator aggregator = new AbilityProfileAggregator();

    @Test
    void noEvidenceProducesUnknownSnapshots() {
        List<AbilitySnapshot> snapshots = aggregator.aggregate(List.of(), NOW);

        assertThat(snapshots).hasSize(AbilityDimension.values().length)
                .allMatch(snapshot -> snapshot.state() == AbilityState.UNKNOWN
                        && snapshot.internalValue() == 0
                        && snapshot.lastObservedAt() == null);
    }

    @Test
    void oneStrongSessionIsStillDeveloping() {
        AbilitySnapshot snapshot = aggregate(AbilityDimension.KNOWLEDGE_JAVA,
                evidence(1, 1, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 5, 1, NOW));

        assertThat(snapshot.state()).isEqualTo(AbilityState.DEVELOPING);
        assertThat(snapshot.distinctSessionCount()).isEqualTo(1);
    }

    @Test
    void contributionFromOneSessionIsCappedAtFive() {
        AbilitySnapshot snapshot = aggregate(AbilityDimension.KNOWLEDGE_JAVA,
                evidence(1, 1, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 5, 1, NOW),
                evidence(2, 1, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 5, 1, NOW));

        assertThat(snapshot.internalValue()).isEqualTo(5.0);
        assertThat(snapshot.distinctSessionCount()).isEqualTo(1);
    }

    @Test
    void evidenceContributionDecaysAtDefinedAgeBands() {
        AbilitySnapshot recent = aggregate(AbilityDimension.KNOWLEDGE_JAVA,
                evidence(1, 1, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 4, 1, NOW.minusDays(8)));
        AbilitySnapshot old = aggregate(AbilityDimension.KNOWLEDGE_MYSQL,
                evidence(2, 2, AbilityDimension.KNOWLEDGE_MYSQL, EvidencePolarity.STRENGTH, 4, 1, NOW.minusDays(91)));

        assertThat(recent.internalValue()).isEqualTo(3.4);
        assertThat(old.internalValue()).isEqualTo(1.4);
    }

    @Test
    void twoPositiveSessionsCanBecomeStable() {
        AbilitySnapshot snapshot = aggregate(AbilityDimension.KNOWLEDGE_JAVA,
                evidence(1, 1, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 2, 0.9, NOW),
                evidence(2, 2, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 2, 0.9, NOW));

        assertThat(snapshot.state()).isEqualTo(AbilityState.STABLE);
        assertThat(snapshot.confidence()).isEqualTo(0.6);
    }

    @Test
    void threeConsistentPositiveSessionsCanBecomeStrong() {
        AbilitySnapshot snapshot = aggregate(AbilityDimension.KNOWLEDGE_JAVA,
                evidence(1, 1, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 2, 1, NOW.minusDays(2)),
                evidence(2, 2, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 2, 1, NOW.minusDays(3)),
                evidence(3, 3, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 2, 1, NOW.minusDays(4)));

        assertThat(snapshot.state()).isEqualTo(AbilityState.STRONG);
    }

    @Test
    void recentHighRiskOverridesOtherwiseStrongHistory() {
        AbilitySnapshot snapshot = aggregate(AbilityDimension.KNOWLEDGE_JAVA,
                evidence(1, 1, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 4, 1, NOW.minusDays(20)),
                evidence(2, 2, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 4, 1, NOW.minusDays(20)),
                evidence(3, 3, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.STRENGTH, 4, 1, NOW.minusDays(20)),
                evidence(4, 4, AbilityDimension.KNOWLEDGE_JAVA, EvidencePolarity.RISK, 4, 0.8, NOW.minusDays(1)));

        assertThat(snapshot.state()).isEqualTo(AbilityState.NEEDS_WORK);
        assertThat(snapshot.riskCount()).isEqualTo(1);
    }

    private AbilitySnapshot aggregate(AbilityDimension dimension, AbilityEvidence... evidence) {
        return aggregator.aggregate(List.of(evidence), NOW).stream()
                .filter(snapshot -> snapshot.dimension() == dimension)
                .findFirst()
                .orElseThrow();
    }

    private AbilityEvidence evidence(long id, long sessionId, AbilityDimension dimension,
                                     EvidencePolarity polarity, int severity, double confidence,
                                     LocalDateTime observedAt) {
        return new AbilityEvidence(id, 1L, dimension.sourceType(), sessionId, id, 1, "e-" + id,
                dimension, polarity, severity, confidence, "证据 " + id, id, id, Map.of(), observedAt);
    }
}
