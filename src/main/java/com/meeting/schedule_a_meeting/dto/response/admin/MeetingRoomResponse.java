package com.meeting.schedule_a_meeting.dto.response.admin;

import lombok.*;

@Data
@Builder
public class MeetingRoomResponse {
    private String id;
    private String name;
    private String location;
    private Integer capacity;
}