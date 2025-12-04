//package com.meeting.schedule_a_meeting.service.users;
//
//import java.time.Instant;
//import java.time.LocalDate;
//import java.util.UUID;
//
//import com.meeting.schedule_a_meeting.entities.Users;
//import com.meeting.schedule_a_meeting.enums.AuthProvider;
//import com.meeting.schedule_a_meeting.enums.ErrorStatus;
//import com.meeting.schedule_a_meeting.enums.Role;
//import com.meeting.schedule_a_meeting.exception.AppException;
//import com.meeting.schedule_a_meeting.repositories.UserRepository;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
//import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.stereotype.Service;
//
//import lombok.AccessLevel;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import lombok.extern.slf4j.Slf4j;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE)
//public class AuthenticationGoogleService {
//
//    final UserRepository userRepository;
//    final OAuth2AuthorizedClientService authorizedClientService;
//
//    public Users loginRegisterByGoogleOAuth2(OAuth2AuthenticationToken auth2AuthenticationToken) {
//        OAuth2User oAuth2User = auth2AuthenticationToken.getPrincipal();
//        String email = oAuth2User.getAttribute("email");
//        String name = oAuth2User.getAttribute("name");
//        String googleId = oAuth2User.getAttribute("sub");
//        String avatar = oAuth2User.getAttribute("picture");
//
//        log.info("USER EMAIL FROM GOOGLE IS {}", email);
//        log.info("USER NAME FROM GOOGLE IS {}", name);
//
//        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
//                auth2AuthenticationToken.getAuthorizedClientRegistrationId(),
//                auth2AuthenticationToken.getName());
//        String accessToken = authorizedClient.getAccessToken().getTokenValue();
//        String refreshToken = (authorizedClient.getRefreshToken() != null)
//                ? authorizedClient.getRefreshToken().getTokenValue()
//                : null;
//        Instant expiresAt = authorizedClient.getAccessToken().getExpiresAt();
//        long expiresIn = expiresAt != null ? (expiresAt.getEpochSecond() - Instant.now().getEpochSecond()) : 3600;
//
//        log.info("ACCESS TOKEN: {}", accessToken);
//        log.info("REFRESH TOKEN: {}", refreshToken != null ? refreshToken : "null");
//        log.info("EXPIRES IN: {} seconds", expiresIn);
//
//        Users existingUser = userRepository.findByEmail(email).orElse(null);
//
//        // TRƯỜNG HỢP 1: Email chưa tồn tại → Tạo mới
//        if (existingUser == null) {
//            Users newUser = new Users();
//            newUser.setName(name);
//            newUser.setEmail(email);
//            newUser.setGoogleId(googleId);
//            newUser.setAvatar_url(avatar);
//            newUser.setAccessToken(accessToken);
//            newUser.setRefreshToken(refreshToken);
//            newUser.setAuthProvider(AuthProvider.GOOGLE);
//            newUser.setRole(Role.USER);
//            newUser.setCreatedAt(LocalDate.now());
//            newUser.setActive(true); // Google login → tự động active
//            userRepository.save(newUser);
//            newUser.setExpiresIn((int) expiresIn);
//            return newUser;
//        }
//
//        // TRƯỜNG HỢP 2: Email tồn tại, nhưng là LOCAL → CẤM login Google
//        if (existingUser.getAuthProvider() == AuthProvider.LOCAL) {
//            log.warn("Attempt to login with Google using LOCAL account email: {}", email);
//            throw new AppException(ErrorStatus.EMAIL_USED_BY_LOCAL);
//        }
//
//        // TRƯỜNG HỢP 3: Email tồn tại, đã là GOOGLE → Cập nhật thông tin
//        if (existingUser.getAuthProvider() == AuthProvider.GOOGLE) {
//            boolean updated = false;
//
//            if (existingUser.getGoogleId() == null || !existingUser.getGoogleId().equals(googleId)) {
//                existingUser.setGoogleId(googleId);
//                updated = true;
//            }
//            if (existingUser.getName() == null || !existingUser.getName().equals(name)) {
//                existingUser.setName(name);
//                updated = true;
//            }
//            if (existingUser.getAvatar_url() == null ||
//                    existingUser.getAvatar_url().contains("google") ||
//                    !existingUser.getAvatar_url().equals(avatar)) {
//                existingUser.setAvatar_url(avatar);
//                updated = true;
//            }
//            if (!accessToken.equals(existingUser.getAccessToken())) {
//                existingUser.setAccessToken(accessToken);
//                updated = true;
//            }
//            if (refreshToken != null &&
//                    (existingUser.getRefreshToken() == null || !existingUser.getRefreshToken().equals(refreshToken))) {
//                existingUser.setRefreshToken(refreshToken);
//                updated = true;
//            }
//
//            if (updated) {
//                userRepository.save(existingUser);
//            }
//            existingUser.setExpiresIn((int) expiresIn);
//            return existingUser;
//        }
//
//        // Không bao giờ đến đây
//        throw new AppException(ErrorStatus.UNAUTHORIZED, "Không thể xác thực người dùng.");
//    }
//
//    public void changePassword(UUID userId, String oldPassword, String newPassword) {
//        Users user = userRepository.findById(userId)
//                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_EXISTED));
//
//        // Kiểm tra nếu tài khoản đăng nhập bằng Google
//        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
//            throw new AppException(ErrorStatus.RESET_PASSWORD_NOT_ALLOWED_FOR_GOOGLE_USER);
//        }
//
//        // Kiểm tra mật khẩu cũ
//        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
//        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
//            throw new AppException(ErrorStatus.INVALID_CREDENTIALS);
//        }
//
//        // Cập nhật mật khẩu mới
//        user.setPassword(passwordEncoder.encode(newPassword));
//        userRepository.save(user);
//    }
//}
