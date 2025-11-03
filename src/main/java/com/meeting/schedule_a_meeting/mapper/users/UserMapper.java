package com.meeting.schedule_a_meeting.mapper.users;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.meeting.schedule_a_meeting.dto.request.users.UserCreationRequest;
import com.meeting.schedule_a_meeting.dto.request.users.UserUpdateRequest;
import com.meeting.schedule_a_meeting.dto.response.users.UserResponse;
import com.meeting.schedule_a_meeting.entities.Users;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", source = "password")
    Users toUser(UserCreationRequest request);

    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    UserResponse toUserResponse(Users user);
    
    void updateUser(@MappingTarget Users user, UserUpdateRequest request);

}
