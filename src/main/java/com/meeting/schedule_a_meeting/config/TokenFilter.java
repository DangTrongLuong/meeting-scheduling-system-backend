package com.meeting.schedule_a_meeting.config;

import java.io.IOException;
import java.security.Key;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;






@Component
public class TokenFilter extends OncePerRequestFilter {


    private static final Key SECRET_KEY = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        String path = request.getRequestURI();
        String method = request.getMethod();

        // Xử lý preflight (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");

            return;
        }

        // Bỏ qua các endpoint công khai
        if (path.startsWith("/api/auth/")
                || path.equals("/")
                || path.startsWith("/uploads/")
                || path.startsWith("/api/admin/")
                || path.startsWith("/api/users/")
                || path.startsWith("/api/meetings/")){
            filterChain.doFilter(request, response);
            return;
        }


        String authHeader = request.getHeader("Authorization");
        String userId = request.getHeader("userId");
        String userName = request.getHeader("userName");
        String role = request.getHeader("role");


        if (path.startsWith("/api/admin/") && !"ADMIN".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Admin role required");
            return;
        }


        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            if (!accessToken.isEmpty()) {
                try {
                    // Validate JWT token
                    Jwts.parserBuilder()
                            .setSigningKey(SECRET_KEY)
                            .build()
                            .parseClaimsJws(accessToken);
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
        } else if (userId != null && userName != null) {
            // Cho phép nếu có userId và userName trong header (dùng session không đáng tin
            // cậy)

            request.setAttribute("userId", userId);
            request.setAttribute("userName", userName);
            request.setAttribute("role", role);
            filterChain.doFilter(request, response);
            return;
        }


        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token");
    }
}