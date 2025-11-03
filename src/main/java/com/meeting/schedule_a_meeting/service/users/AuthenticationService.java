package com.meeting.schedule_a_meeting.service.users;

import java.security.Key;
import java.util.Date;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.meeting.schedule_a_meeting.dto.request.users.AuthenticationRequest;
import com.meeting.schedule_a_meeting.dto.response.users.AuthenticationResponse;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.repositories.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    UserRepository userRepository;
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    private static final long EXPIRATION_TIME = 3600_000; // 1 giờ (milliseconds);
    private static final String DEFAULT_AVATAR_URL = "http://localhost:8080/uploads/avatars/user-avatar.png";

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        Users user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_EXISTED));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorStatus.INVALID_CREDENTIALS);
        }

        if (user.getAvatar_url() == null || user.getAvatar_url().isEmpty()) {
            user.setAvatar_url(DEFAULT_AVATAR_URL);
            userRepository.save(user);
        }
        // Tạo JWT
        String accessToken = Jwts.builder()
                .setSubject(user.getEmail().toLowerCase())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();

        return AuthenticationResponse.builder()
                .authenticated(true)
                .accessToken(accessToken)
                .expiresIn(EXPIRATION_TIME / 1000)
                .id(user.getId())
                .name(user.getName() != null ? user.getName() : "User")
                .email(user.getEmail().toLowerCase())
                .avatarUrl(user.getAvatar_url() != null ? user.getAvatar_url() : "")
                .role(user.getRole().name())
                .backgroundUrl(user.getBackground_url() != null ? user.getBackground_url() : "")
                .createdAt(user.getCreatedAt())
                .authProvider("LOCAL")
                .build();
    }
}
