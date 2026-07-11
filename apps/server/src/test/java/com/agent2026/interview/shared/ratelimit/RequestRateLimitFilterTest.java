package com.agent2026.interview.shared.ratelimit;

import com.agent2026.interview.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RequestRateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void limitsAllConfiguredLlmAndResourcePostPaths() throws Exception {
        String[] paths = {
                "/api/project-profiles",
                "/api/project-profiles/12/analyze",
                "/api/interview-sessions/21/answers",
                "/api/interview-sessions/21/turns"
        };

        for (String path : paths) {
            RequestRateLimitFilter filter = filter(1, true);
            AtomicInteger chainCalls = new AtomicInteger();

            MockHttpServletResponse first = execute(filter, path, "203.0.113.8", "10.0.0.1", chainCalls);
            MockHttpServletResponse second = execute(filter, path, "203.0.113.8", "10.0.0.2", chainCalls);

            assertThat(first.getStatus()).as(path).isEqualTo(200);
            assertThat(second.getStatus()).as(path).isEqualTo(429);
            assertThat(chainCalls.get()).as(path).isEqualTo(1);
        }
    }

    @Test
    void quotaIsSharedAcrossProtectedEndpointsForSameRealIp() throws Exception {
        RequestRateLimitFilter filter = filter(2, true);
        AtomicInteger chainCalls = new AtomicInteger();

        execute(filter, "/api/project-profiles", "198.51.100.9", "10.0.0.1", chainCalls);
        execute(filter, "/api/project-profiles/1/analyze", "198.51.100.9", "10.0.0.2", chainCalls);
        MockHttpServletResponse blocked = execute(filter, "/api/interview-sessions/1/turns",
                "198.51.100.9", "10.0.0.3", chainCalls);

        JsonNode body = objectMapper.readTree(blocked.getContentAsString());
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(body.path("code").asInt()).isEqualTo(ErrorCode.RATE_LIMITED.getCode());
        assertThat(body.path("msg").asText()).isEqualTo(ErrorCode.RATE_LIMITED.getMessage());
        assertThat(body.path("data").isNull()).isTrue();
        assertThat(chainCalls.get()).isEqualTo(2);
    }

    @Test
    void invalidRealIpFallsBackToRemoteAddress() throws Exception {
        RequestRateLimitFilter filter = filter(1, true);
        AtomicInteger chainCalls = new AtomicInteger();

        execute(filter, "/api/project-profiles", "attacker-controlled-value", "192.0.2.5", chainCalls);
        MockHttpServletResponse blocked = execute(filter, "/api/project-profiles",
                "another-invalid-value", "192.0.2.5", chainCalls);

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(chainCalls.get()).isEqualTo(1);
    }

    @Test
    void unrelatedMethodsAndPathsAreNeverLimited() throws Exception {
        RequestRateLimitFilter filter = filter(1, true);
        AtomicInteger chainCalls = new AtomicInteger();

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = request("GET", "/api/project-profiles/1", "203.0.113.1", "10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, countingChain(chainCalls));
            assertThat(response.getStatus()).isEqualTo(200);
        }

        for (String path : new String[]{"/api/interview-sessions", "/api/project-profiles/1/confirm"}) {
            MockHttpServletRequest request = request("POST", path, "203.0.113.1", "10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, countingChain(chainCalls));
            assertThat(response.getStatus()).as(path).isEqualTo(200);
        }

        assertThat(chainCalls.get()).isEqualTo(7);
    }

    @Test
    void filterCanBeDisabledForControlledEnvironments() throws Exception {
        RequestRateLimitFilter filter = filter(1, false);
        AtomicInteger chainCalls = new AtomicInteger();

        execute(filter, "/api/project-profiles", "203.0.113.1", "10.0.0.1", chainCalls);
        MockHttpServletResponse second = execute(filter, "/api/project-profiles",
                "203.0.113.1", "10.0.0.1", chainCalls);

        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(chainCalls.get()).isEqualTo(2);
    }

    private RequestRateLimitFilter filter(int requestsPerWindow, boolean enabled) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(enabled);
        properties.setRequestsPerWindow(requestsPerWindow);
        properties.setWindow(Duration.ofMinutes(1));
        properties.setMaxTrackedClients(100);
        return new RequestRateLimitFilter(properties,
                new InMemoryFixedWindowRateLimiter(properties), objectMapper);
    }

    private MockHttpServletResponse execute(RequestRateLimitFilter filter,
                                            String path,
                                            String realIp,
                                            String remoteAddress,
                                            AtomicInteger chainCalls) throws Exception {
        MockHttpServletRequest request = request("POST", path, realIp, remoteAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, countingChain(chainCalls));
        return response;
    }

    private MockHttpServletRequest request(String method,
                                           String path,
                                           String realIp,
                                           String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader(RequestRateLimitFilter.REAL_IP_HEADER, realIp);
        request.setRemoteAddr(remoteAddress);
        return request;
    }

    private FilterChain countingChain(AtomicInteger calls) {
        return (request, response) -> calls.incrementAndGet();
    }
}
