package com.meeting.schedule_a_meeting.controllers.users;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.meeting.schedule_a_meeting.service.users.AuthenticationService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    UserService userService;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    AuthenticationService authenticationService;


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


    // UserController.java

    // UserController.java - trong @PostMapping("/register-bulk")
    @PostMapping("/register-bulk")
    public ResponseEntity<?> createBulkUsers(@RequestBody List<UserCreationRequest> requests) {
        try {
            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No users provided"
                ));
            }

            // Chuẩn hoá emails từ payload
            List<String> emails = requests.stream()
                    .map(r -> r.getEmail() == null ? "" : r.getEmail().trim().toLowerCase())
                    .toList();

            // Trùng trong payload
            Map<String, Long> counts = emails.stream()
                    .filter(e -> !e.isEmpty())
                    .collect(java.util.stream.Collectors.groupingBy(e -> e, java.util.stream.Collectors.counting()));
            List<String> duplicateInPayload = counts.entrySet().stream()
                    .filter(e -> e.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .toList();

            // Trùng trong DB
            List<String> duplicateInDb = emails.stream()
                    .filter(e -> !e.isEmpty() && userService.checkMail(e))
                    .toList();

            if (!duplicateInPayload.isEmpty() || !duplicateInDb.isEmpty()) {
                // Ghép message chi tiết
                StringBuilder sb = new StringBuilder("Duplicate emails detected. No accounts were created.");
                if (!duplicateInPayload.isEmpty()) {
                    sb.append(" Duplicate in form: ").append(String.join(", ", duplicateInPayload)).append(".");
                }
                if (!duplicateInDb.isEmpty()) {
                    sb.append(" Already exists in system: ").append(String.join(", ", duplicateInDb)).append(".");
                }

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("message", sb.toString());
                resp.put("duplicateInPayload", duplicateInPayload);
                resp.put("duplicateInDb", duplicateInDb);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
            }

            // Không có trùng -> tạo người dùng
            Map<String, Object> result = userService.createBulkUsers(requests);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Bulk registration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Server error during bulk creation"
            ));
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

    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean exists = userService.checkMail(email);
        return ResponseEntity.ok(exists);
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

    @PostMapping("/send-first-login-code")
    public ResponseEntity<?> sendFirstLoginCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        authenticationService.sendFirstLoginVerificationCode(email);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Verification code sent to your email. Please check your inbox.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-first-login-code")
    public ResponseEntity<?> verifyFirstLoginCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");

        boolean isValid = authenticationService.verifyFirstLoginCode(email, code);

        Map<String, Object> response = new HashMap<>();
        if (isValid) {
            response.put("status", "success");
            response.put("message", "Code verified successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Invalid or expired code");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/check-first-login")
    public ResponseEntity<Map<String, Object>> checkFirstLogin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        Map<String, Object> response = new HashMap<>();

        Users user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            response.put("firstLogin", false);
            return ResponseEntity.ok(response);
        }

        response.put("firstLogin", user.isFirstLogin());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-password-first-login")
    public ResponseEntity<Map<String, Object>> updatePasswordFirstLogin(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        String newPassword = body.get("newPassword");

        Map<String, Object> response = new HashMap<>();

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isFirstLogin()) {
            response.put("success", false);
            response.put("message", "This is not your first login");
            return ResponseEntity.badRequest().body(response);
        }

        // Validate password
        if (newPassword == null || newPassword.length() < 8 ||
                !newPassword.matches(".*[a-z].*") ||
                !newPassword.matches(".*[A-Z].*") ||
                !newPassword.matches(".*\\d.*") ||
                !newPassword.matches(".*[@$!%*?&].*")) {
            response.put("success", false);
            response.put("message",
                    "Password must be 8+ characters with uppercase, lowercase, number and special character");
            return ResponseEntity.badRequest().body(response);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFirstLogin(false);
        user.setActive(true);
        userRepository.save(user);

        response.put("success", true);
        response.put("message", "Password updated successfully!");
        return ResponseEntity.ok(response);
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
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));

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

    @PostMapping("/upload-avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            @RequestParam("email") String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            Users user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Email not found"));

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get("uploads/avatars/" + fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());

            user.setAvatar_url("/uploads/avatars/" + fileName);
            userRepository.save(user);

            response.put("success", true);
            response.put("avatar_url", user.getAvatar_url());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error uploading avatar: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/upload-background")
    public ResponseEntity<Map<String, Object>> uploadBackground(
            @RequestParam("background") MultipartFile file,
            @RequestParam("email") String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            Users user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Email not found"));

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get("uploads/backgrounds/" + fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());

            user.setBackground_url("/uploads/backgrounds/" + fileName);
            userRepository.save(user);

            response.put("success", true);
            response.put("background_url", user.getBackground_url());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error uploading background: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/remove-background")
    public ResponseEntity<Map<String, Object>> removeBackground(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = request.get("email");
            Users user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Email not found"));

            if (user.getBackground_url() != null) {
                String fileName = user.getBackground_url().substring(user.getBackground_url().lastIndexOf("/") + 1);
                Path filePath = Paths.get("uploads/backgrounds/" + fileName);
                Files.deleteIfExists(filePath);
                user.setBackground_url(null);
                userRepository.save(user);
            }

            response.put("success", true);
            response.put("message", "Background removed successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error removing background: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/page")
    public Page<Users> getUsers(@RequestParam(defaultValue = "0") int page) {
        return userService.getUsersByPage(page);
    }
}