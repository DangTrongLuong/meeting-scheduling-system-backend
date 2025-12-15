package com.meeting.schedule_a_meeting.dto.response.admin.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class MeetingStatusDistributionDTO {
    private Long scheduled;
    private Long cancelled;
    private Long pending_approval;
    private String period;
}
