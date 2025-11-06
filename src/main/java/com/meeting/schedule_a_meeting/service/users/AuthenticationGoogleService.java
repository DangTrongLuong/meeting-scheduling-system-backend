package com.meeting.schedule_a_meeting.service.users;


import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.AuthProvider;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.enums.Role;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationGoogleService {

    final UserRepository userRepository;
    final OAuth2AuthorizedClientService authorizedClientService;

    public Users loginRegisterByGoogleOAuth2(OAuth2AuthenticationToken auth2AuthenticationToken) {
        OAuth2User oAuth2User = auth2AuthenticationToken.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String avatar = oAuth2User.getAttribute("picture");

        log.info("USER EMAIL FROM GOOGLE IS {}", email);
        log.info("USER NAME FROM GOOGLE IS {}", name);

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                auth2AuthenticationToken.getAuthorizedClientRegistrationId(),
                auth2AuthenticationToken.getName());
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        String refreshToken = (authorizedClient.getRefreshToken() != null)
                ? authorizedClient.getRefreshToken().getTokenValue()
                : null;
        // Calculate expiresIn from access token expiration
        Instant expiresAt = authorizedClient.getAccessToken().getExpiresAt();
        long expiresIn = expiresAt != null ? (expiresAt.getEpochSecond() - Instant.now().getEpochSecond()) : 3600;

        log.info("ACCESS TOKEN: {}", accessToken);
        log.info("REFRESH TOKEN: {}", refreshToken != null ? refreshToken : "null (Google did not provide a refresh token)");
        log.info("EXPIRES IN: {} seconds", expiresIn);

        Users user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new Users();
            user.setName(name);
            user.setEmail(email);
            user.setGoogleId(googleId);
            user.setAvatar_url(avatar);
            user.setAccessToken(accessToken);
            user.setRefreshToken(refreshToken);
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setRole(Role.USER);
            user.setCreatedAt(LocalDate.now());
            userRepository.save(user);
        } else {
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
            }
            if (user.getName() == null || !user.getName().equals(name)) {
                user.setName(name);
            }
            if (user.getAvatar_url() != null && !user.getAvatar_url().contains("google")) {
                // Giữ avatar tùy chỉnh nếu đã upload
            } else {
                user.setAvatar_url(avatar);
            }
            if (user.getAccessToken() == null || !user.getAccessToken().equals(accessToken)) {
                user.setAccessToken(accessToken);
            }
            if (refreshToken != null && (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken))) {
                user.setRefreshToken(refreshToken);
            }
            userRepository.save(user);
        }


        user.setExpiresIn((int) expiresIn);
        return user;
    }
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_EXISTED));

        // Kiểm tra nếu tài khoản đăng nhập bằng Google
        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new AppException(ErrorStatus.RESET_PASSWORD_NOT_ALLOWED_FOR_GOOGLE_USER);
        }

        // Kiểm tra mật khẩu cũ
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AppException(ErrorStatus.INVALID_CREDENTIALS);
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

