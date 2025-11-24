
package com.meeting.schedule_a_meeting.dto.response.users.meeting;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MeetingReminderDTO {
    private String title;
    private LocalDateTime startTime;
    private String roomName;
}