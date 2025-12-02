package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.config.JwtTokenUtil;
import com.meeting.schedule_a_meeting.dto.request.users.AuthenticationRequest;
import com.meeting.schedule_a_meeting.dto.response.users.AuthenticationResponse;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    UserRepository userRepository;
    JwtTokenUtil jwtTokenUtil;
    TwoFactorAuthService twoFactorAuthService;

    private static final String DEFAULT_AVATAR_URL = "http://localhost:8080/uploads/avatars/user-avatar.png";

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        Users user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_EXISTED));

        // if (!user.isActive()) {
        // throw new AppException(ErrorStatus.USER_NOT_ACTIVATED);
        // }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorStatus.INVALID_CREDENTIALS);
        }

        // YÊU CẦU 2FA
        if (user.isTwoFactorEnabled()) {
            throw new AppException(ErrorStatus.TWO_FACTOR_REQUIRED);
        }

        // Login bình thường nếu chưa bật 2FA
        return generateTokenResponse(user);
    }

    // API mới: xác minh 2FA rồi mới cấp token
    public AuthenticationResponse verify2FACodeAndLogin(String email, String code) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_EXISTED));

        if (!user.isTwoFactorEnabled()) {
            throw new AppException(ErrorStatus.TWO_FACTOR_NOT_ENABLED);
        }

        if (!twoFactorAuthService.isValidCode(user.getTwoFactorSecret(), code)) {
            throw new AppException(ErrorStatus.INVALID_2FA_CODE);
        }

        return generateTokenResponse(user);
    }

    private AuthenticationResponse generateTokenResponse(Users user) {
        String accessToken = jwtTokenUtil.generateToken(
                user.getEmail(), user.getId().toString(), user.getRole().name());

        if (user.getAvatar_url() == null || user.getAvatar_url().isEmpty()) {
            user.setAvatar_url(DEFAULT_AVATAR_URL);
            userRepository.save(user);
        }

        return AuthenticationResponse.builder()
                .authenticated(true)
                .accessToken(accessToken)
                .expiresIn(jwtTokenUtil.getExpirationSeconds())
                .id(user.getId())
                .name(user.getName() != null ? user.getName() : "User")
                .email(user.getEmail().toLowerCase())
                .avatarUrl(user.getAvatar_url())
                .role(user.getRole().name())
                .backgroundUrl(user.getBackground_url())
                .createdAt(user.getCreatedAt())
                .authProvider("LOCAL")
                .build();
    }
}