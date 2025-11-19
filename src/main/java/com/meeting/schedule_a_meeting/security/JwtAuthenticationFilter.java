// package com.meeting.schedule_a_meeting.security;

// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.security.Keys;
// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import
// org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.stereotype.Component;
// import org.springframework.util.StringUtils;
// import org.springframework.web.filter.OncePerRequestFilter;

// import java.io.IOException;
// import java.nio.charset.StandardCharsets;
// import java.security.Key;
// import java.util.Collections;

// @Component
// @Slf4j
// public class JwtAuthenticationFilter extends OncePerRequestFilter {

// @Value("${jwt.secret}")
// private String jwtSecret;

// private Key getSigningKey() {
// return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
// }

// @Override
// protected void doFilterInternal(HttpServletRequest request,
// HttpServletResponse response,
// FilterChain filterChain) throws ServletException, IOException {

// String requestURI = request.getRequestURI();
// String method = request.getMethod();

// log.debug("JWT Filter: {} {}", method, requestURI);

// try {
// String jwt = getJwtFromRequest(request);

// if (StringUtils.hasText(jwt)) {
// log.debug("JWT token found, validating...");

// if (validateToken(jwt)) {
// Claims claims = Jwts.parserBuilder()
// .setSigningKey(getSigningKey())
// .build()
// .parseClaimsJws(jwt)
// .getBody();

// String userId = claims.get("userId", String.class);
// String email = claims.getSubject();
// String role = claims.get("role", String.class);

// log.info("JWT Valid - User: {}, Email: {}, Role: {}", userId, email, role);

// UsernamePasswordAuthenticationToken authentication = new
// UsernamePasswordAuthenticationToken(
// userId,
// null,
// Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));

// SecurityContextHolder.getContext().setAuthentication(authentication);
// }
// }
// } catch (Exception ex) {
// log.error("JWT Error: {}", ex.getMessage());
// }

// filterChain.doFilter(request, response);
// }

// private String getJwtFromRequest(HttpServletRequest request) {
// String bearerToken = request.getHeader("Authorization");

// if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
// String token = bearerToken.substring(7);

// long dotCount = token.chars().filter(ch -> ch == '.').count();
// if (dotCount != 2) {
// log.warn("Invalid JWT format: expected 2 dots, found {}", dotCount);
// return null;
// }

// return token;
// }

// return null;
// }

// private boolean validateToken(String token) {
// try {
// Jwts.parserBuilder()
// .setSigningKey(getSigningKey())
// .build()
// .parseClaimsJws(token);
// return true;
// } catch (Exception e) {
// log.error("Token validation failed: {}", e.getMessage());
// }
// return false;
// }
// }