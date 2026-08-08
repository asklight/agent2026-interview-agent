package com.agent2026.interview.identity.security;

import com.agent2026.interview.identity.infrastructure.jwt.JwtTokenService;
import com.agent2026.interview.identity.infrastructure.jwt.VerifiedJwt;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String PREFIX = "Bearer ";
    private final JwtTokenService tokens;

    public JwtAuthenticationFilter(JwtTokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(PREFIX)) {
            try {
                VerifiedJwt jwt = tokens.verifyAccess(authorization.substring(PREFIX.length()).trim());
                AuthenticatedUser principal = new AuthenticatedUser(jwt.userId());
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, List.of()));
            } catch (JWTVerificationException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
