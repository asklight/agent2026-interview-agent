package com.agent2026.interview.algorithmpractice.infrastructure.llm;

import com.agent2026.interview.algorithmpractice.domain.AlgorithmEvaluationContext;
import com.agent2026.interview.algorithmpractice.domain.AlgorithmStage;
import com.agent2026.interview.client.TjuLlmClient;
import com.agent2026.interview.common.LlmApiException;
import com.agent2026.interview.vo.LlmTestVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TjuLlmAlgorithmTurnEvaluatorTest {
    @Test
    void acceptsOnlyTheFixedFiveDimensionsIncludingNull() {
        TjuLlmClient client = mock(TjuLlmClient.class);
        when(client.chat(anyString())).thenReturn(response("""
                {"scores":{"correctness":80,"optimization":null,"complexity":null,"edgeCases":null,
                "communication":70},"strengths":["方向正确"],"gaps":[],"evidence":["使用哈希表"],
                "suggestedFollowUp":"如何处理重复值？"}
                """));

        var evaluation = new TjuLlmAlgorithmTurnEvaluator(client, new ObjectMapper()).evaluate(context());

        assertThat(evaluation.scores()).hasSize(5).containsEntry("optimization", null);
        assertThat(evaluation.evidence()).containsExactly("使用哈希表");
        assertThat(evaluation.modelResponseHash()).hasSize(64);
    }

    @Test
    void retriesMalformedJsonOnceThenFails() {
        TjuLlmClient client = mock(TjuLlmClient.class);
        when(client.chat(anyString())).thenReturn(response("not-json"));

        assertThatThrownBy(() -> new TjuLlmAlgorithmTurnEvaluator(client, new ObjectMapper()).evaluate(context()))
                .isInstanceOf(LlmApiException.class);
        verify(client, times(2)).chat(anyString());
    }

    private AlgorithmEvaluationContext context() {
        return new AlgorithmEvaluationContext("两数之和", "找到目标和", List.of("有唯一答案"),
                List.of("哈希表"), AlgorithmStage.BASELINE_SOLUTION, List.of(), "我会使用哈希表");
    }

    private LlmTestVO response(String content) {
        return new LlmTestVO("model", content, 0, "now");
    }
}
