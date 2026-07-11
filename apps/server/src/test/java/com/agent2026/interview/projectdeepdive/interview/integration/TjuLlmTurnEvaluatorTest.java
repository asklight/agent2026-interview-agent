package com.agent2026.interview.projectdeepdive.interview.integration;

import com.agent2026.interview.client.TjuLlmClient;
import com.agent2026.interview.common.LlmApiException;
import com.agent2026.interview.projectdeepdive.interview.domain.PlannedProbe;
import com.agent2026.interview.vo.LlmTestVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TjuLlmTurnEvaluatorTest {
    @Test void acceptsOnlyFixedSixDimensionSchemaIncludingNull() {
        TjuLlmClient client = mock(TjuLlmClient.class);
        when(client.chat(anyString())).thenReturn(new LlmTestVO("m", """
                {"scores":{"authenticity":80,"ownership":70,"technicalDepth":null,"tradeoffReasoning":null,
                "engineeringAwareness":null,"communication":75},"hitPoints":[],"missingPoints":[],"weaknesses":[],
                "evidence":[],"riskFlags":["需要进一步验证"],"decision":"FOLLOW_UP","suggestedFollowUp":"请补充过程"}
                """, 0, "now"));
        var result = new TjuLlmTurnEvaluator(client, new ObjectMapper()).evaluate(context());
        assertThat(result.scores()).containsEntry("technicalDepth", null).hasSize(6);
    }

    @Test void rejectsAccusatoryRiskEvenAfterRepair() {
        TjuLlmClient client = mock(TjuLlmClient.class);
        String bad = """
                {"scores":{"authenticity":10,"ownership":10,"technicalDepth":null,"tradeoffReasoning":null,
                "engineeringAwareness":null,"communication":20},"hitPoints":[],"missingPoints":[],"weaknesses":[],
                "evidence":[],"riskFlags":["候选人造假"],"decision":"FOLLOW_UP","suggestedFollowUp":"继续"}
                """;
        when(client.chat(anyString())).thenReturn(new LlmTestVO("m", bad, 0, "now"));
        assertThatThrownBy(() -> new TjuLlmTurnEvaluator(client, new ObjectMapper()).evaluate(context()))
                .isInstanceOf(LlmApiException.class);
        verify(client, times(2)).chat(anyString());
    }

    private TurnEvaluationContext context() {
        return new TurnEvaluationContext("summary", new PlannedProbe("p", 1L, "OWNERSHIP", "objective"),
                List.of(), "answer", List.of());
    }
}
