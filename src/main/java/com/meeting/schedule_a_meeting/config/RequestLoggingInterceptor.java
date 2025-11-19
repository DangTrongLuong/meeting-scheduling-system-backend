package com.meeting.schedule_a_meeting.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j

public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("INCOMING REQUEST");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Method: {} {}", request.getMethod(), request.getRequestURI());
        log.info("Query String: {}", request.getQueryString());
        log.info("Content-Type: {}", request.getContentType());
        log.info("Remote Address: {}", request.getRemoteAddr());
        log.info("Handler: {}", handler.getClass().getName());

        // Log important headers
        String auth = request.getHeader("Authorization");
        if (auth != null) {
            log.info("Authorization: {}...", auth.substring(0, Math.min(30, auth.length())));
        } else {
            log.warn("⚠No Authorization header");
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("REQUEST COMPLETED");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Status: {}", response.getStatus());
        log.info("Content-Type: {}", response.getContentType());
        if (ex != null) {
            log.error("Exception: {}", ex.getMessage());
        }
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
}