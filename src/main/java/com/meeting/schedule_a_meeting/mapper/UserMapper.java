package com.meeting.schedule_a_meeting.mapper;

import com.meeting.schedule_a_meeting.dto.request.users.UserCreationRequest;
import com.meeting.schedule_a_meeting.dto.response.users.UserResponse;
import com.meeting.schedule_a_meeting.entities.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", source= "password")
    Users toUser(UserCreationRequest request);

    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    UserResponse toUserResponse(Users user);
}
