package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.dto.request.users.UserCreationRequest;
import com.meeting.schedule_a_meeting.dto.response.users.UserResponse;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.exception.DuplicateEmailException;
import com.meeting.schedule_a_meeting.mapper.users.UserMapper;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(UserCreationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email đã tồn tại");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Users user = userMapper.toEntity(request, encodedPassword);
        userRepository.save(user);
        return userMapper.toResponse(user);
    }
}
