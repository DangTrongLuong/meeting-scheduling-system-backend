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

    private static final String DEFAULT_AVATAR_URL = "http://localhost:8080/uploads/avatars/user-avatar.png";

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        Users user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_EXISTED));

        if (!user.isActive()) {
            throw new AppException(ErrorStatus.USER_NOT_ACTIVATED);
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorStatus.INVALID_CREDENTIALS);
        }

        if (user.getAvatar_url() == null || user.getAvatar_url().isEmpty()) {
            user.setAvatar_url(DEFAULT_AVATAR_URL);
            userRepository.save(user);
        }

        String accessToken = jwtTokenUtil.generateToken(user.getEmail(), user.getId().toString(), user.getRole().name());

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