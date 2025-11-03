package com.meeting.schedule_a_meeting.service.users;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.meeting.schedule_a_meeting.dto.request.users.UserCreationRequest;
import com.meeting.schedule_a_meeting.dto.request.users.UserUpdateRequest;
import com.meeting.schedule_a_meeting.dto.response.users.UserResponse;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.AuthProvider;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.enums.Role;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.mapper.users.UserMapper;
import com.meeting.schedule_a_meeting.repositories.UserRepository;

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

    private static final String DEFAULT_AVATAR_URL = "http://localhost:8080/uploads/avatars/user-avatar.png";

    public Users createUser(UserCreationRequest request) {

        Users user = userMapper.toUser(request);
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorStatus.USER_EXISTED);
        }
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider(AuthProvider.LOCAL); // Thêm giá trị mặc định
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDate.now());
        user.setAvatar_url(DEFAULT_AVATAR_URL);
        return userRepository.save(user);
    }

    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public UserResponse updateUserRequest(UUID id, UserUpdateRequest request) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOTFOUND));
        userMapper.updateUser(user, request);

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    public List<Users> getUsers() {
        return userRepository.findAll();
    }

    public UserResponse getUser(UUID id) {
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.USER_NOTFOUND)));
    }

}
