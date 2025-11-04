package com.meeting.schedule_a_meeting.controllers.users;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.meeting.schedule_a_meeting.dto.request.users.UserCreationRequest;
import com.meeting.schedule_a_meeting.dto.request.users.UserUpdateRequest;
import com.meeting.schedule_a_meeting.dto.response.users.UserResponse;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import com.meeting.schedule_a_meeting.service.users.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> createUser(@RequestBody UserCreationRequest request) {
        try {
            log.info("Processing registration for email: {}", request.getEmail());
            Users user = userService.createUser(request);
            log.info("User created successfully: {}", user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", user);
            response.put("message", "Registration successful");

            return ResponseEntity.ok(response);
        } catch (AppException e) {
            log.error("Registration failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Unexpected error occurred");
            errorResponse.put("message", "Unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/get-users")
    List<Users> getUsers() {
        return userService.getUsers();
    }

    @GetMapping("/get-users/{userId}")
    UserResponse getUser(@PathVariable("userId") UUID userId) {
        return userService.getUser(userId);
    }

    @PutMapping("/update-user/{userId}")
    UserResponse updateUser(@PathVariable("userId") UUID userId, @RequestBody UserUpdateRequest request) {
        return userService.updateUserRequest(userId, request);
    }

    @DeleteMapping("/delete-user/{userId}")
    String deleteUser(@PathVariable("userId") UUID id) {
        userService.deleteUser(id);
        return "User has been deleted";
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        try {
            userService.logout(bearerToken);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            log.error("Logout failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyAccount(
            @RequestParam("email") String email,
            @RequestParam("token") String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "Verification token is missing."));
        }

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOTFOUND));

        if (user.isActive()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Account is already activated."));
        }

        if (user.getVerificationToken() == null || !user.getVerificationToken().equals(token)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "Invalid or expired verification token."));
        }

        user.setActive(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Account activated successfully! You can now login."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        userService.sendResetCode(email);
        return ResponseEntity.ok(Map.of("success", true, "message", "Reset code sent to your email."));
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        userService.verifyResetCode(email, code);
        return ResponseEntity.ok(Map.of("success", true, "message", "Code verified successfully."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        userService.resetPassword(email, newPassword);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successfully."));
    }
}