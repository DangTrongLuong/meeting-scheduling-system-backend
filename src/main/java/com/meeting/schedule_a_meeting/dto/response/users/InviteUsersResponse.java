package com.meeting.schedule_a_meeting.dto.response.users;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteUsersResponse {
    private Long meetingId;
    private List<String> invitedEmails;
}
