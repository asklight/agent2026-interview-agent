package com.agent2026.interview.projectdeepdive.infrastructure.llm;

import com.agent2026.interview.client.TjuLlmClient;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfileAnalysis;
import com.agent2026.interview.projectdeepdive.domain.service.ProjectProfileAnalysisValidator;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.agent2026.interview.vo.LlmTestVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TjuLlmProjectProfileAnalyzerTest {

    private static final String SOURCE = "订单平台使用 Spring Boot 和 Redis。我负责缓存模块，将 P95 从 300ms 降到 120ms。";

    private TjuLlmClient client;
    private TjuLlmProjectProfileAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        client = mock(TjuLlmClient.class);
        analyzer = new TjuLlmProjectProfileAnalyzer(client, new ObjectMapper(),
                new ProjectProfileAnalysisValidator());
        when(client.isConfigured()).thenReturn(true);
    }

    @Test
    void parsesAndValidatesStructuredAnalysis() {
        when(client.chat(anyString())).thenReturn(response(validJson()));

        ProjectProfileAnalysis result = analyzer.analyze(SOURCE);

        assertThat(result.projectName()).isEqualTo("订单平台");
        assertThat(result.claims()).hasSize(1);
        verify(client).chat(anyString());
    }

    @Test
    void repairsMalformedOutputOnlyOnce() {
        when(client.chat(anyString())).thenReturn(response("不是 JSON"), response(validJson()));

        ProjectProfileAnalysis result = analyzer.analyze(SOURCE);

        assertThat(result.techStack()).contains("Redis");
        verify(client, times(2)).chat(anyString());
    }

    @Test
    void rejectsSecondInvalidOutput() {
        when(client.chat(anyString())).thenReturn(response("坏 JSON"), response("仍然是坏 JSON"));

        assertThatThrownBy(() -> analyzer.analyze(SOURCE))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LLM_RESPONSE_INVALID));
        verify(client, times(2)).chat(anyString());
    }

    private LlmTestVO response(String content) {
        return new LlmTestVO("test", content, 0, "now");
    }

    private String validJson() {
        return """
                {
                  "projectName": "订单平台",
                  "summary": "订单平台缓存优化项目",
                  "techStack": ["Spring Boot", "Redis"],
                  "responsibilities": ["缓存模块"],
                  "metrics": ["P95 从 300ms 降到 120ms"],
                  "architecture": [],
                  "uncertainties": ["压测口径待确认"],
                  "claims": [{
                    "claimType": "PERFORMANCE_IMPROVEMENT",
                    "statement": "通过 Redis 缓存降低 P95",
                    "sourceFragment": "将 P95 从 300ms 降到 120ms",
                    "relatedTechnologies": ["Redis"],
                    "expectedEvidence": ["压测口径", "缓存命中率"],
                    "riskLevel": "HIGH"
                  }]
                }
                """;
    }
}
