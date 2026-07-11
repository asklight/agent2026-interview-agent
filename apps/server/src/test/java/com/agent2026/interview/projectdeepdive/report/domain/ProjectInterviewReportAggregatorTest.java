package com.agent2026.interview.projectdeepdive.report.domain;

import com.agent2026.interview.projectdeepdive.report.api.ProjectInterviewReportResponse;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class ProjectInterviewReportAggregatorTest {
    @Test void uncoveredDimensionsAreNotAssessedAndExcludedFromTotal() {
        ReportEvaluationFact fact = new ReportEvaluationFact(30L, 20L, 10L, "probe-1", "OWNERSHIP", "回答",
                Map.of("ownership", 80, "communication", 60), List.of("职责边界清楚"), List.of("缺少协作细节"),
                List.of(), List.of(), List.of("证据仍需补充"));
        ProjectInterviewReportResponse report = new ProjectInterviewReportAggregator()
                .aggregate(1L, List.of(fact), LocalDateTime.now());

        assertThat(report.totalScore()).isEqualTo(70.0);
        assertThat(report.coverageRate()).isEqualTo(33.3);
        assertThat(report.dimensions()).filteredOn(d -> "NOT_ASSESSED".equals(d.status())).hasSize(4);
        assertThat(report.strengths().get(0).claimId()).isEqualTo(10L);
        assertThat(report.strengths().get(0).candidateTurnId()).isEqualTo(20L);
        assertThat(report.strengths().get(0).evaluationId()).isEqualTo(30L);
        assertThat(report.rounds()).hasSize(1);
    }

    @Test void activeFinishWithoutAnswersProducesZeroCoverageNotZeroScore() {
        ProjectInterviewReportResponse report = new ProjectInterviewReportAggregator()
                .aggregate(1L, List.of(), LocalDateTime.now());
        assertThat(report.totalScore()).isNull();
        assertThat(report.coverageRate()).isZero();
        assertThat(report.dimensions()).allMatch(d -> "NOT_ASSESSED".equals(d.status()) && d.score() == null);
    }
}
