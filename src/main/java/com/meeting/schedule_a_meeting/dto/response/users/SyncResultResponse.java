package com.meeting.schedule_a_meeting.dto.response.users;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SyncResultResponse {
    private int totalMeetings;
    private int syncedCount;
    private int skippedCount;
    private List<String> skippedTitles;
    private List<String> failedTitles;
}
