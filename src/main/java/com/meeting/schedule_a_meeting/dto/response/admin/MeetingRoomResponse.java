package com.meeting.schedule_a_meeting.dto.response.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRoomResponse {
    private Long id;
    private String name;
    private String location;
    private Integer capacity;
}
