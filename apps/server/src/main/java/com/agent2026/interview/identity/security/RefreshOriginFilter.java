package com.agent2026.interview.identity.security;

import com.agent2026.interview.common.Result;
import com.agent2026.interview.identity.infrastructure.jwt.JwtProperties;
import com.agent2026.interview.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import org.springframework.util.StringUtils;

@Component
public class RefreshOriginFilter extends OncePerRequestFilter {
    private static final Set<String> PROTECTED_PATHS = Set.of("/api/auth/refresh", "/api/auth/logout");
    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public RefreshOriginFilter(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !PROTECTED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        String allowedOrigin = properties.getAllowedOrigin();
        if (!StringUtils.hasText(allowedOrigin)) {
            String scheme = request.getHeader("X-Forwarded-Proto");
            if (!StringUtils.hasText(scheme)) scheme = request.getScheme();
            allowedOrigin = scheme + "://" + request.getHeader(HttpHeaders.HOST);
        }
        if (origin != null && !origin.equals(allowedOrigin)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    Result.error(ErrorCode.AUTH_REFRESH_TOKEN_INVALID.getCode(), "请求来源不受信任"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
