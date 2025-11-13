package com.meeting.schedule_a_meeting.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;

@Component
public class TokenFilter extends OncePerRequestFilter {

    // Use a secure key from application properties in production
    private static final Key SECRET_KEY = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // ✅ Handle CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(method)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, userId, Content-Type, role");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            return;
        }

        // ✅ Skip token validation for public endpoints
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Extract headers
        String authHeader = request.getHeader("Authorization");
        String userId = request.getHeader("userId");
        String userName = request.getHeader("userName");
        String role = request.getHeader("role");

        // ✅ Role-based access for admin endpoints
        if (path.startsWith("/api/admin/") && !"ADMIN".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Admin role required");
            return;
        }

        // ✅ Validate JWT token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            try {
                Jwts.parserBuilder()
                        .setSigningKey(SECRET_KEY)
                        .build()
                        .parseClaimsJws(accessToken);

                // Attach user info to request for downstream use
                request.setAttribute("userId", userId);
                request.setAttribute("userName", userName);
                request.setAttribute("role", role);

                filterChain.doFilter(request, response);
                return;
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token: " + e.getMessage());
                return;
            }
        }

        // ✅ Fallback: allow if userId and userName exist (not recommended for production)
        if (userId != null && userName != null) {
            request.setAttribute("userId", userId);
            request.setAttribute("userName", userName);
            request.setAttribute("role", role);
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ If no valid token or user info
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token");
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/meetings/")
                || path.startsWith("/uploads/")
                || path.equals("/");
    }
}