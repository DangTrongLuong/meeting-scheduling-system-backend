package com.meeting.schedule_a_meeting.dto.request.admin;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MeetingRoomRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Location is required")
    @Size(max = 200)
    private String location;

    @NotNull
    @Positive
    private Integer capacity;
}