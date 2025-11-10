package com.meeting.schedule_a_meeting.dto.request.users;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteUsersRequest {
    private List<String> emails;
}
