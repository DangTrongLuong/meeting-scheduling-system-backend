package com.meeting.schedule_a_meeting.dto.request.users.meetting;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMeetingRequest {

    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    private String description;

    private String startTime;

    private String endTime;

    private String roomId;

    private List<ParticipantRequest> participants;

    private List<DeviceBorrowRequest> borrowedDevices;
}
