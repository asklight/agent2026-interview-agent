package com.agent2026.interview.shared.ratelimit;

import com.agent2026.interview.common.Result;
import com.agent2026.interview.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestRateLimitFilter extends OncePerRequestFilter {

    static final String REAL_IP_HEADER = "X-Real-IP";

    private static final Pattern PROJECT_ANALYZE =
            Pattern.compile("^/api/project-profiles/\\d+/analyze/?$");
    private static final Pattern INTERVIEW_LLM_OPERATION =
            Pattern.compile("^/api/interview-sessions/\\d+/(?:answers|turns)/?$");
    private static final Pattern IP_ADDRESS = Pattern.compile("^[0-9a-fA-F:.]{2,64}$");

    private final RateLimitProperties properties;
    private final InMemoryFixedWindowRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RequestRateLimitFilter(RateLimitProperties properties,
                                  InMemoryFixedWindowRateLimiter rateLimiter,
                                  ObjectMapper objectMapper) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled() || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = pathWithinApplication(request);
        return !("/api/project-profiles".equals(path)
                || "/api/project-profiles/".equals(path)
                || PROJECT_ANALYZE.matcher(path).matches()
                || INTERVIEW_LLM_OPERATION.matcher(path).matches());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        if (rateLimiter.tryAcquire(clientIp, System.currentTimeMillis())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", Long.toString(rateLimiter.retryAfterSeconds()));
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getWriter(),
                Result.error(ErrorCode.RATE_LIMITED.getCode(), ErrorCode.RATE_LIMITED.getMessage()));
    }

    String resolveClientIp(HttpServletRequest request) {
        String realIp = normalizedIp(request.getHeader(REAL_IP_HEADER));
        if (realIp != null) {
            return realIp;
        }
        String remoteAddress = normalizedIp(request.getRemoteAddr());
        return remoteAddress == null ? "unknown" : remoteAddress;
    }

    private String normalizedIp(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        String value = candidate.trim();
        if (!IP_ADDRESS.matcher(value).matches()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
