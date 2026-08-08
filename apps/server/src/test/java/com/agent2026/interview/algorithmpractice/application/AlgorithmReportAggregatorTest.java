package com.agent2026.interview.algorithmpractice.application;

import com.agent2026.interview.algorithmpractice.domain.AlgorithmEvaluation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AlgorithmReportAggregatorTest {
    @Test
    void leavesUncoveredDimensionsUnassessedAndKeepsEvidenceReferences() {
        AlgorithmEvaluation evaluation = new AlgorithmEvaluation(
                Map.of("correctness", 80, "communication", 60),
                List.of("方案方向正确"), List.of("没有说明边界"), List.of("使用哈希表一次扫描"),
                "输入有重复值时怎么办？", "hash", false);
        var fact = new AlgorithmReportAggregator.EvaluationFact(
                31L, 21L, "BASELINE_SOLUTION", "我会使用哈希表一次扫描", evaluation);

        var report = new AlgorithmReportAggregator().aggregate(11L, List.of(fact), LocalDateTime.now());

        assertThat(report.overallScore()).isEqualTo(70.0);
        assertThat(report.coverage()).isEqualTo(40.0);
        assertThat(report.completionStatus()).isEqualTo("PARTIAL");
        assertThat(report.dimensions()).filteredOn(item -> "NOT_ASSESSED".equals(item.status())).hasSize(3);
        assertThat(report.strengths().get(0).candidateTurnId()).isEqualTo(21L);
        assertThat(report.strengths().get(0).evaluationId()).isEqualTo(31L);
        assertThat(report.strengths().get(0).candidateEvidence()).isEqualTo("使用哈希表一次扫描");
    }

    @Test
    void noAnswersProducesPartialReportWithoutFakeScore() {
        var report = new AlgorithmReportAggregator().aggregate(11L, List.of(), LocalDateTime.now());
        assertThat(report.overallScore()).isNull();
        assertThat(report.coverage()).isZero();
        assertThat(report.rounds()).isEmpty();
    }
}
