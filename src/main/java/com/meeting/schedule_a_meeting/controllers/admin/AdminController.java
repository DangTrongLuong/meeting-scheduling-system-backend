package com.meeting.schedule_a_meeting.controllers.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meeting.schedule_a_meeting.dto.request.admin.AdminRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.AdminResponse;
import com.meeting.schedule_a_meeting.enums.Role;
import com.meeting.schedule_a_meeting.exception.AppException;
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

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        boolean isValid = adminService.checkCurrentPassword(email, currentPassword);
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Current password is incorrect."));
        }
        adminService.checkEmailGG(email);

        adminService.resetPassword(email, newPassword);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successfully."));

    }

    @PostMapping("/change-user-password")
    public ResponseEntity<?> changeUserPassword(@RequestBody Map<String, String> request) {
        String userIdStr = request.get("userId");
        String newPassword = request.get("newPassword");

        if (userIdStr == null || newPassword == null || newPassword.trim().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "User ID and new password (min 8 chars) are required"));
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid user ID format"));
        }

        try {
            adminService.changeUserPasswordByAdmin(userId, newPassword);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password changed successfully"));
        } catch (AppException e) {
            return ResponseEntity.status(e.getErrorStatus().getStatus())
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Trong AdminController.java (cùng file có /login, /change-user-password)

    @PatchMapping("/update-user-role")
    public ResponseEntity<?> updateUserRole(@RequestBody Map<String, String> request) {
        String userIdStr = request.get("userId");
        String newRole = request.get("role");

        if (userIdStr == null || newRole == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "userId and role required"));
        }

        UUID userId = UUID.fromString(userIdStr);
        Role role = Role.valueOf(newRole.toUpperCase());

        adminService.updateUserRole(userId, role);
        return ResponseEntity.ok(Map.of("success", true, "message", "Role updated"));
    }
};
