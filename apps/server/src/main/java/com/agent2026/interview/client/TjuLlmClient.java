package com.agent2026.interview.client;

import com.agent2026.interview.common.LlmApiException;
import com.agent2026.interview.config.TjuLlmProperties;
import com.agent2026.interview.vo.LlmTestVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class TjuLlmClient {

    private static final int CODE_LLM_CONFIG_ERROR = 50010;
    private static final int CODE_LLM_UNAUTHORIZED = 40110;
    private static final int CODE_LLM_RATE_LIMITED = 42910;
    private static final int CODE_LLM_TIMEOUT = 50410;
    private static final int CODE_LLM_SERVICE_ERROR = 50210;
    private static final int CODE_LLM_RESPONSE_ERROR = 50211;

    private final TjuLlmProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public TjuLlmClient(TjuLlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
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

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri(properties.getApiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw toLlmApiException(response.getStatusCode().value());
                    })
                    .body(String.class);
        } catch (LlmApiException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new LlmApiException(CODE_LLM_TIMEOUT, "学校 tju-llm API 请求超时或网络不可达，请稍后重试", ex);
        } catch (RestClientException ex) {
            throw new LlmApiException(CODE_LLM_SERVICE_ERROR, "学校 tju-llm API 调用失败，请稍后重试", ex);
        }

        return parseResponse(responseBody);
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.getApiUrl())
                && StringUtils.hasText(properties.getApiKey())
                && StringUtils.hasText(properties.getModel());
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getApiUrl())) {
            throw new LlmApiException(CODE_LLM_CONFIG_ERROR, "未配置 TJU_LLM_API_URL");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new LlmApiException(CODE_LLM_CONFIG_ERROR, "未配置 TJU_LLM_API_KEY");
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new LlmApiException(CODE_LLM_CONFIG_ERROR, "未配置 TJU_LLM_MODEL");
        }
    }

    private LlmApiException toLlmApiException(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return new LlmApiException(CODE_LLM_UNAUTHORIZED, "学校 tju-llm API Key 无效、过期或无权限，请检查后端环境变量");
        }
        if (statusCode == 429) {
            return new LlmApiException(CODE_LLM_RATE_LIMITED, "学校 tju-llm API 调用过于频繁或额度受限，请稍后再试");
        }
        if (statusCode >= 500) {
            return new LlmApiException(CODE_LLM_SERVICE_ERROR, "学校 tju-llm API 服务异常，请稍后重试");
        }
        return new LlmApiException(CODE_LLM_SERVICE_ERROR, "学校 tju-llm API 调用失败，状态码：" + statusCode);
    }

    private LlmTestVO parseResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new LlmApiException(CODE_LLM_RESPONSE_ERROR, "学校 tju-llm API 返回为空");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("");
            if (!StringUtils.hasText(content)) {
                throw new LlmApiException(CODE_LLM_RESPONSE_ERROR, "学校 tju-llm API 返回内容为空");
            }
            int totalTokens = root.path("usage").path("total_tokens").asInt(0);
            return new LlmTestVO(properties.getModel(), content, totalTokens, Instant.now().toString());
        } catch (IOException e) {
            throw new LlmApiException(CODE_LLM_RESPONSE_ERROR, "学校 tju-llm API 响应解析失败", e);
        }
    }
}
