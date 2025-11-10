package com.meeting.schedule_a_meeting.dto.request.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRoomRequest {
    private String name;
    private String location;
    private Integer capacity;
}