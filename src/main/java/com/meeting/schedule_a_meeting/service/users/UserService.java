package com.meeting.schedule_a_meeting.service.users;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meeting.schedule_a_meeting.dto.request.users.UserCreationRequest;
import com.meeting.schedule_a_meeting.dto.request.users.UserUpdateRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.MeetingRoomResponse;
import com.meeting.schedule_a_meeting.dto.response.users.UserResponse;
import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.AuthProvider;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.enums.Role;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.mapper.admin.MeetingRoomMapper;
import com.meeting.schedule_a_meeting.mapper.users.UserMapper;
import com.meeting.schedule_a_meeting.repositories.MeetingRoomRepository;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingDeviceRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingParticipantRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserService {

    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;
    final UserMapper userMapper;
    final EmailService emailService;
    private final MeetingRoomRepository meetingRoomRepository;
    private final MeetingRoomMapper meetingRoomMapper;
    final MeetingRepository meetingRepository;
    final MeetingParticipantRepository participantRepository;
    final MeetingDeviceRepository meetingDeviceRepository;

    private static final String DEFAULT_AVATAR_URL = "http://localhost:8080/uploads/avatars/user-avatar.png";

    public Users createUser(UserCreationRequest request) {

        Users user = userMapper.toUser(request);
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorStatus.EMAIL_EXISTED);
        }
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDate.now());
        user.setAvatar_url(DEFAULT_AVATAR_URL);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setActive(false);

        user = userRepository.save(user);
        String verifyLink = "http://localhost:5173/verify?email="
                + user.getEmail()
                + "&token="
                + user.getVerificationToken();
        emailService.sendVerificationEmail(user.getEmail(), verifyLink);

        return user;
    }

    @Transactional
    public Map<String, Object> createBulkUsers(List<UserCreationRequest> requests) {
        List<String> createdEmails = new ArrayList<>();
        List<String> failedEmails = new ArrayList<>();
        int successCount = 0;

        for (UserCreationRequest request : requests) {

            String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
            if (email.isEmpty()) {
                throw new AppException(ErrorStatus.INVALID_TOKEN, "Email is empty in payload");
            }
            if (userRepository.existsByEmail(email)) {
                // Abort toàn bộ
                throw new AppException(ErrorStatus.EMAIL_EXISTED, "Duplicate email found in DB: " + email);
            }

            try {
                // Kiểm tra email đã tồn tại
                if (userRepository.existsByEmail(request.getEmail())) {
                    failedEmails.add(request.getEmail() + " (already exists)");
                    continue;
                }

                Users user = userMapper.toUser(request);
                PasswordEncoder encoder = new BCryptPasswordEncoder(10);
                user.setPassword(encoder.encode(request.getPassword()));
                user.setAuthProvider(AuthProvider.LOCAL);
                user.setRole(Role.USER);
                user.setCreatedAt(LocalDate.now());
                user.setAvatar_url(DEFAULT_AVATAR_URL);
                user.setVerificationToken(UUID.randomUUID().toString());
                user.setActive(false);
                user.setFirstLogin(true);

                userRepository.save(user);
                createdEmails.add(request.getEmail());

                // // Gửi email xác nhận
                // String verifyLink = "http://localhost:5173/verify?email=" + user.getEmail() +
                // "&token="
                // + user.getVerificationToken();
                // emailService.sendVerificationEmail(user.getEmail(), verifyLink);

                successCount++;
            } catch (Exception e) {
                failedEmails.add(request.getEmail() + " (error)");
                log.error("Failed to create user: {}", request.getEmail(), e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", !createdEmails.isEmpty());
        result.put("createdCount", successCount);
        result.put("createdEmails", createdEmails);
        result.put("failedEmails", failedEmails);
        result.put("message", successCount > 0
                ? String.format("Successfully created %d user(s)", successCount)
                : "No users were created");

        return result;
    }

    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public UserResponse updateUserRequest(UUID id, UserUpdateRequest request) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));
        userMapper.updateUser(user, request);

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));

        List<Meeting> createdMeetings = meetingRepository.findByCreatorId(id);
        for (Meeting m : createdMeetings) {
            participantRepository.deleteByMeetingId(m.getId());
            meetingDeviceRepository.deleteByMeetingId(m.getId());
            meetingRepository.delete(m);
        }

        participantRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    public List<Users> getUsers() {
        return userRepository.findAll();
    }

    public UserResponse getUser(UUID id) {
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND)));
    }

    public boolean checkMail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void logout(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);

            Users user = userRepository.findByAccessToken(token)
                    .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));

            user.setAccessToken(null);
            user.setRefreshToken(null);
            userRepository.save(user);

            SecurityContextHolder.clearContext();

            log.info("User logged out successfully: {}", user.getEmail());
        } else {
            throw new AppException(ErrorStatus.INVALID_TOKEN);
        }
    }

    public void sendResetCode(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_EXISTED));

        if (user.getAuthProvider() == AuthProvider.GOOGLE || user.getGoogleId() != null) {
            throw new AppException(ErrorStatus.RESET_PASSWORD_NOT_ALLOWED_FOR_GOOGLE_USER);
        }

        String code = generateResetCode();
        user.setResetCode(code);
        user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(10));
        user.setResetAttempts(0);
        userRepository.save(user);

        emailService.sendResetCodeEmail(email, code);
    }

    private String generateResetCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    public boolean verifyResetCode(String email, String code) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_EXISTED));

        if (user.getResetCode() == null || LocalDateTime.now().isAfter(user.getResetCodeExpiry())) {
            throw new AppException(ErrorStatus.INVALID_TOKEN, "Code expired");
        }

        if (!user.getResetCode().equals(code)) {
            int attempts = user.getResetAttempts() + 1;
            user.setResetAttempts(attempts);
            if (attempts >= 5) {
                user.setResetCode(null);
                user.setResetCodeExpiry(null);
                userRepository.save(user);
                throw new AppException(ErrorStatus.INVALID_TOKEN,
                        "Maximum attempts reached. Please request a new code.");
            }
            userRepository.save(user);
            throw new AppException(ErrorStatus.INVALID_TOKEN, "Invalid code. " + (5 - attempts) + " attempts left.");
        }

        return true;
    }

    public void resetPassword(String email, String newPassword) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_EXISTED));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetCode(null);
        user.setResetCodeExpiry(null);
        user.setResetAttempts(0);
        userRepository.save(user);
    }

    public MeetingRoomResponse getById(String id) {
        MeetingRoom room = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));
        return meetingRoomMapper.toResponse(room);
    }

    public List<MeetingRoomResponse> searchByName(String name) {
        return meetingRoomRepository.findAll().stream()
                .filter(r -> r.getName().toLowerCase().contains(name.toLowerCase()))
                .map(meetingRoomMapper::toResponse)
                .toList();
    }

    public Page<Users> getUsersByPage(int page) {
        int pageSize = 10;
        PageRequest pageable = PageRequest.of(page, pageSize);
        return userRepository.findAll(pageable);
    }
}
