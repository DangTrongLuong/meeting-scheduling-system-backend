package com.meeting.schedule_a_meeting.controllers.admin;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meeting.schedule_a_meeting.dto.request.admin.AdminRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.AdminResponse;
import com.meeting.schedule_a_meeting.service.admin.AdminService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/admin/auth")
public class AdminController {

    AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateAdmin(@RequestBody AdminRequest request) {
        AdminResponse data = adminService.authenticateAdmin(request);

        Map<String, Object> body = new HashMap<>();
        body.put("status", "success");
        body.put("message", "Admin login successful");
        body.put("data", data);

        return ResponseEntity.ok(body);
    }
}
