package com.meeting.schedule_a_meeting.controllers.users;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meeting.schedule_a_meeting.dto.request.users.AuthenticationRequest;
import com.meeting.schedule_a_meeting.dto.response.users.AuthenticationResponse;
import com.meeting.schedule_a_meeting.service.users.AuthenticationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/auth")
public class AuthenticationController {

    final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse data = authenticationService.authenticate(request);

        Map<String, Object> body = new HashMap<>();
        body.put("status", "success");
        body.put("message", "Login successful");
        body.put("data", data);

        return ResponseEntity.ok(body);
    }

}
