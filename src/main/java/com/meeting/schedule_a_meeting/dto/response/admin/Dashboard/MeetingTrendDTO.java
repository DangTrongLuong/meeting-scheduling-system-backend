package com.meeting.schedule_a_meeting.dto.response.admin.Dashboard;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class MeetingTrendDTO {
    private Map<String, Integer> data;
    private String period;
}
