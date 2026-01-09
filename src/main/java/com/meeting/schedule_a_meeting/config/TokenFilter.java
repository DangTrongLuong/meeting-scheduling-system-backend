package com.meeting.schedule_a_meeting.config;

import java.io.IOException;
import java.security.Key;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

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
                || path.startsWith("/api/meetings/")
                || path.startsWith("/api/google-calendar/")
                || path.startsWith("/ws/")) {
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
            if (jwtTokenUtil.validateToken(accessToken)) {
                try {

                    Claims claims = jwtTokenUtil.extractClaims(accessToken);

                    userId = (String) claims.get("userId");
                    role = (String) claims.get("role");
                    userName = claims.getSubject();

                    // --- Chèn Authentication vào SecurityContext ---
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null,
                            List.of(new SimpleGrantedAuthority(role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    // --------------------------------------------

                    // Gán attribute cho request
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

            request.setAttribute("userId", userId);
            request.setAttribute("userName", userName);
            request.setAttribute("role", role);
            filterChain.doFilter(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token");

    }
}