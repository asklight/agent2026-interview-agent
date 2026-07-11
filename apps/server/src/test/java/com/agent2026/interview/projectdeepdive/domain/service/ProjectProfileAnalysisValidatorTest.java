package com.agent2026.interview.projectdeepdive.domain.service;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimRiskLevel;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimType;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfileAnalysis;
import com.agent2026.interview.shared.error.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectProfileAnalysisValidatorTest {

    private final ProjectProfileAnalysisValidator validator = new ProjectProfileAnalysisValidator();
    private final String source = "订单平台使用 Spring Boot 和 Redis。我负责缓存模块，将 P95 从 300ms 降到 120ms。";

    @Test
    void acceptsFactsWithTraceableSourceFragments() {
        ProjectProfileAnalysis validated = validator.validate(validAnalysis(), source);

        assertThat(validated.techStack()).containsExactly("Spring Boot", "Redis");
        assertThat(validated.claims()).hasSize(1);
    }

    @Test
    void rejectsTechnologyNotMentionedByUser() {
        ProjectProfileAnalysis invalid = new ProjectProfileAnalysis("订单平台", "摘要",
                List.of("Kafka"), List.of("缓存模块"), List.of("P95 从 300ms 降到 120ms"), List.of(), List.of(),
                validAnalysis().claims());

        assertThatThrownBy(() -> validator.validate(invalid, source))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Kafka");
    }

    @Test
    void rejectsInventedMetricValue() {
        ProjectProfileAnalysis invalid = new ProjectProfileAnalysis("订单平台", "摘要",
                List.of("Redis"), List.of("缓存模块"), List.of("P95 降到 20ms"), List.of(), List.of(),
                validAnalysis().claims());

        assertThatThrownBy(() -> validator.validate(invalid, source))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("20");
    }

    @Test
    void rejectsMetricTypeThatReusesAnExistingNumber() {
        ProjectProfileAnalysis invalid = new ProjectProfileAnalysis("订单平台", "摘要",
                List.of("Redis"), List.of("缓存模块"), List.of("QPS 提升到 300"), List.of(), List.of(),
                validAnalysis().claims());

        assertThatThrownBy(() -> validator.validate(invalid, source))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("QPS");
    }

    @Test
    void rejectsResponsibilityNotSupportedBySource() {
        ProjectProfileAnalysis invalid = new ProjectProfileAnalysis("订单平台", "摘要",
                List.of("Redis"), List.of("主导整体架构"), List.of("P95 从 300ms 降到 120ms"), List.of(), List.of(),
                validAnalysis().claims());

        assertThatThrownBy(() -> validator.validate(invalid, source))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("主导整体架构");
    }

    private ProjectProfileAnalysis validAnalysis() {
        ProjectClaim claim = new ProjectClaim(null, null, ProjectClaimType.PERFORMANCE_IMPROVEMENT,
                "通过 Redis 缓存降低 P95", "将 P95 从 300ms 降到 120ms",
                List.of("Redis"), List.of("压测口径", "缓存命中率"), ProjectClaimRiskLevel.HIGH, false, null);
        return new ProjectProfileAnalysis("订单平台", "订单平台缓存优化项目",
                List.of("Spring Boot", "Redis"), List.of("缓存模块"),
                List.of("P95 从 300ms 降到 120ms"), List.of(), List.of(), List.of(claim));
    }
}
