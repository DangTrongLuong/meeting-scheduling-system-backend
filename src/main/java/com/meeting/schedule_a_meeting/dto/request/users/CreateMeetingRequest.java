package com.meeting.schedule_a_meeting.dto.request.users;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMeetingRequest {
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String room;
}
