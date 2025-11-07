package com.meeting.schedule_a_meeting.service.admin;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;

import com.meeting.schedule_a_meeting.enums.AuthProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.meeting.schedule_a_meeting.dto.request.admin.AdminRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.AdminResponse;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.enums.Role;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.repositories.AdminRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminService {

    AdminRepository adminRepository;
    final PasswordEncoder passwordEncoder;
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    private static final long EXPIRATION_TIME = 3600_000; // 1 giờ (milliseconds)
    private static final String DEFAULT_AVATAR_URL = "http://localhost:8080/uploads/avatars/admin-avatar.png";

    public AdminResponse authenticateAdmin(AdminRequest request) {
        // Tìm user theo email
        Users admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_EXISTED));

        // Kiểm tra role phải là ADMIN
        if (admin.getRole() != Role.ADMIN) {
            throw new AppException(ErrorStatus.UNAUTHORIZED);
        }

        // Kiểm tra tài khoản đã active chưa
        if (!admin.isActive()) {
            throw new AppException(ErrorStatus.USER_NOT_ACTIVATED);
        }

        // Xác thực mật khẩu
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new AppException(ErrorStatus.INVALID_CREDENTIALS);
        }

        // Set default avatar nếu chưa có
        if (admin.getAvatar_url() == null || admin.getAvatar_url().isEmpty()) {
            admin.setAvatar_url(DEFAULT_AVATAR_URL);
            adminRepository.save(admin);
        }

        // Tạo JWT token
        String accessToken = Jwts.builder()
                .setSubject(admin.getEmail().toLowerCase())
                .claim("userId", admin.getId())
                .claim("role", admin.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();

        // Trả về response
        return AdminResponse.builder()
                .authenticated(true)
                .id(admin.getId())
                .accessToken(accessToken)
                .expiresIn(EXPIRATION_TIME / 1000)
                .name(admin.getName() != null ? admin.getName() : "ADMIN")
                .email(admin.getEmail().toLowerCase())
                .avatarUrl(admin.getAvatar_url() != null ? admin.getAvatar_url() : "")
                .role(admin.getRole().name())
                .age(admin.getAge())
                .address(admin.getAddress() != null ? admin.getAddress() : "")
                .createdAt(admin.getCreatedAt())
                .backgroundUrl(admin.getBackground_url() != null ? admin.getBackground_url() : "")
                .authProvider(admin.getAuthProvider() != null ? admin.getAuthProvider().name() : "LOCAL")
                .build();
    }

    public void checkEmailGG(String email) {
        Users admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorStatus.ADMIN_NOT_EXISTED));


        if (admin.getAuthProvider() == AuthProvider.GOOGLE || admin.getGoogleId() != null) {
            throw new AppException(ErrorStatus.RESET_PASSWORD_NOT_ALLOWED_FOR_GOOGLE_USER);
        }

    }

    public boolean checkCurrentPassword(String email, String currentPassword) {
        Users admin = adminRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorStatus.ADMIN_NOT_EXISTED));;
        if (admin == null) return false;

        return passwordEncoder.matches(currentPassword, admin.getPassword());
    }

    public void resetPassword(String email, String newPassword) {
        Users admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorStatus.ADMIN_NOT_EXISTED));

        admin.setPassword(passwordEncoder.encode(newPassword));
        adminRepository.save(admin);
    }
}
