package com.meeting.schedule_a_meeting.dto.request.users;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMeetingRequest {
    private String title;
    private String room;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String roomId;
    private String status;
    private List<String> invitedEmails;

}