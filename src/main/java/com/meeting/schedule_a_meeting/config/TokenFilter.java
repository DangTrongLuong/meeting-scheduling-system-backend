package com.meeting.schedule_a_meeting.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    public TokenFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Allow preflight requests
        if ("OPTIONS".equalsIgnoreCase(method)) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Skip public endpoints
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate JWT token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenUtil.validateToken(token)) {
                Claims claims = jwtTokenUtil.extractClaims(token);

                // Attach user info to request
                request.setAttribute("userId", claims.get("userId"));
                request.setAttribute("role", claims.get("role"));
                request.setAttribute("email", claims.getSubject());

                // Role-based check for admin endpoints
                if (path.startsWith("/api/admin/") && !"ADMIN".equals(claims.get("role"))) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Admin role required");
                    return;
                }

                filterChain.doFilter(request, response);
                return;
            }
        }

        // If token is missing or invalid
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token");
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/")
                || path.equals("/")
                || path.startsWith("/uploads/")
                || path.startsWith("/api/public/")
                || path.startsWith("/api/meetings/");
    }
}
