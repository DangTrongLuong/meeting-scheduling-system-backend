package com.meeting.schedule_a_meeting.controllers.users;

import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import com.meeting.schedule_a_meeting.service.users.TwoFactorAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/2fa")
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final UserRepository userRepository;

    // Gọi khi lần đầu bật 2FA → tạo secret + QR
    @PostMapping("/setup")
    public ResponseEntity<?> setup(@RequestParam String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isTwoFactorEnabled()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "2FA already enabled"));
        }

        try {
            String secret = twoFactorAuthService.generateNewSecret();
            String qrCode = twoFactorAuthService.generateQrCodeDataUri(secret, email);

            // Lưu tạm secret (chưa bật chính thức)
            user.setTwoFactorSecret(secret);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "qrCode", qrCode,
                    "secret", secret));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to generate QR code"));
        }
    }

    // Xác nhận mã → bật 2FA vĩnh viễn
    @PostMapping("/verify-setup")
    public ResponseEntity<?> verifyAndEnable(@RequestParam String email, @RequestParam String code) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isTwoFactorEnabled()) {
            return ResponseEntity.badRequest().body("2FA already enabled");
        }

        if (user.getTwoFactorSecret() == null) {
            return ResponseEntity.badRequest().body("No setup in progress");
        }

        if (twoFactorAuthService.isValidCode(user.getTwoFactorSecret(), code)) {
            user.setTwoFactorEnabled(true);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true));
        }

        return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Invalid code"));
    }

    // Tắt 2FA (từ profile)
    @PostMapping("/disable")
    public ResponseEntity<?> disable(@RequestParam String email) {
        Users user = userRepository.findByEmail(email).orElseThrow();
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
        return ResponseEntity.ok("2FA disabled");
    }

    // Kiểm tra trạng thái 2FA
    @GetMapping("/status")
    public ResponseEntity<?> check2FAStatus(@RequestParam String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
                "twoFactorEnabled", user.isTwoFactorEnabled(),
                "hasSecret", user.getTwoFactorSecret() != null));
    }
}
