package com.meeting.schedule_a_meeting.dto.request.users;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingRoomScheduleRequest {
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
}