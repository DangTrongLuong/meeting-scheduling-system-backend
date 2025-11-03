package com.meeting.schedule_a_meeting.mapper.users;

import com.meeting.schedule_a_meeting.dto.request.users.UserCreationRequest;
import com.meeting.schedule_a_meeting.dto.response.users.UserResponse;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.AuthProvider;
import com.meeting.schedule_a_meeting.enums.Role;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class UserMapper {

    public Users toEntity(UserCreationRequest request, String encodedPassword) {
        Users user = new Users();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(encodedPassword);
        user.setCreatedAt(LocalDate.now());
        user.setRole(Role.USER);
        user.setAuthProvider(AuthProvider.LOCAL);
        return user;
    }

    public UserResponse toResponse(Users user) {
        return UserResponse.builder()
                .userId(user.getUser_id())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }
}