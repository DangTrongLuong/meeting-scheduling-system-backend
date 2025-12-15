package com.meeting.schedule_a_meeting.dto.response.admin.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class UserStatisticsDTO {
    private Long totalUsers;
    private Long activeUsers;
    private Long inactiveUsers;
}
