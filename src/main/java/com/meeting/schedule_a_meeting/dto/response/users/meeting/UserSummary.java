package com.meeting.schedule_a_meeting.dto.response.users.meeting;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {
    private UUID id;
    private String name;
    private String email;
    private String avatarUrl;
}
