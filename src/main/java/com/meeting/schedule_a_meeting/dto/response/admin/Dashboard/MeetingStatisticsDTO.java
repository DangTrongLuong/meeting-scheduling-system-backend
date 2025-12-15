package com.meeting.schedule_a_meeting.dto.response.admin.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingStatisticsDTO {
    private Long totalMeetings;
    private Long scheduledMeetings;
    private Long cancelledMeetings;
    private Long pendingMeetings;
    private String period;
}
