package com.meeting.schedule_a_meeting.dto.response.users;

import com.meeting.schedule_a_meeting.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String userId;
    String name;
    String email;
    String avatarUrl;
    Role role;
}