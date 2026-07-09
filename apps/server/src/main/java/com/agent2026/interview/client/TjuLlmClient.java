package com.agent2026.interview.client;

import com.agent2026.interview.config.TjuLlmProperties;
import com.agent2026.interview.vo.LlmTestVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class TjuLlmClient {

    private final TjuLlmProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public TjuLlmClient(TjuLlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public LlmTestVO chat(String userMessage) {
        validateConfig();

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是一个严谨的 Java 后端面试训练助手。"),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        String responseBody = restClient.post()
                .uri(properties.getApiUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("tju-llm request failed: status=" + response.getStatusCode().value());
                })
                .body(String.class);

        return parseResponse(responseBody);
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getApiUrl())) {
            throw new IllegalStateException("TJU_LLM_API_URL is not configured");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("TJU_LLM_API_KEY is not configured");
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new IllegalStateException("TJU_LLM_MODEL is not configured");
        }
    }

    private LlmTestVO parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("");
            int totalTokens = root.path("usage").path("total_tokens").asInt(0);
            return new LlmTestVO(properties.getModel(), content, totalTokens, Instant.now().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse tju-llm response", e);
        }
    }
}
