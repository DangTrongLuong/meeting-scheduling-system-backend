package com.meeting.schedule_a_meeting.dto.response.users;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingResponse {
    private Long id;
    private String title;
    private String room;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String roomName;
    private String createdBy;
    private List<String> invitedEmails;
}