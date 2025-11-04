package com.meeting.schedule_a_meeting.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandle {
    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> handleAppException(AppException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", ex.getCustomMessage());
        body.put("error", ex.getErrorStatus().name());

        return ResponseEntity
                .status(ex.getErrorStatus().getStatus())
                .body(body);
    }
}
