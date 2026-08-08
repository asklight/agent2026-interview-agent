package com.agent2026.interview.projectdeepdive.interview.knowledge;

import com.agent2026.interview.config.RagToolkitProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "rag-toolkit", name = "enabled", havingValue = "true")
public class RagToolkitVectorRetrievalService implements VectorRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RagToolkitVectorRetrievalService.class);

    private final RagToolkitProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public RagToolkitVectorRetrievalService(RagToolkitProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, buildRestClient(properties));
    }

    RagToolkitVectorRetrievalService(RagToolkitProperties properties, ObjectMapper objectMapper,
                                     RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    @Override
    public RetrievalContext retrieve(String query) {
        if (!properties.isEnabled() || !StringUtils.hasText(query)) {
            return new RetrievalContext(List.of(), false);
        }

        try {
            String responseBody = restClient.post()
                    .uri(searchUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(this::addSearchToken)
                    .body(Map.of(
                            "query", query,
                            "top_k", properties.getTopK(),
                            "filters", Map.of()))
                    .retrieve()
                    .body(String.class);
            return parseResponse(responseBody);
        } catch (RestClientException ex) {
            log.warn("rag-toolkit retrieval degraded: profile={}, reason={}",
                    properties.getProfile(), ex.getClass().getSimpleName());
            log.debug("rag-toolkit retrieval request failed", ex);
            return new RetrievalContext(List.of(), true);
        } catch (RuntimeException ex) {
            log.warn("rag-toolkit retrieval response degraded: profile={}, reason={}",
                    properties.getProfile(), ex.getClass().getSimpleName());
            log.debug("rag-toolkit retrieval response was invalid", ex);
            return new RetrievalContext(List.of(), true);
        }
    }

    private RetrievalContext parseResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalArgumentException("rag-toolkit response is empty");
        }
        try {
            JsonNode hits = objectMapper.readTree(responseBody).path("hits");
            if (!hits.isArray()) {
                throw new IllegalArgumentException("rag-toolkit response hits is not an array");
            }
            List<String> snippets = new ArrayList<>();
            for (JsonNode hit : hits) {
                String content = hit.path("content").asText("").trim();
                if (StringUtils.hasText(content)) {
                    snippets.add(content);
                }
            }
            return new RetrievalContext(snippets, false);
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException("rag-toolkit response is invalid", ex);
        }
    }

    private void addSearchToken(HttpHeaders headers) {
        if (StringUtils.hasText(properties.getSearchToken())) {
            headers.set("X-Rag-Token", properties.getSearchToken());
        }
    }

    private String searchUri() {
        String baseUrl = properties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("rag-toolkit base-url is blank");
        }
        return baseUrl.replaceAll("/+\\z", "") + "/search";
    }

    private static RestClient buildRestClient(RagToolkitProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(nonNullDuration(properties.getConnectTimeout(), Duration.ofSeconds(2)));
        requestFactory.setReadTimeout(nonNullDuration(properties.getReadTimeout(), Duration.ofSeconds(5)));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private static Duration nonNullDuration(Duration duration, Duration fallback) {
        return duration == null || duration.isNegative() || duration.isZero() ? fallback : duration;
    }
}
