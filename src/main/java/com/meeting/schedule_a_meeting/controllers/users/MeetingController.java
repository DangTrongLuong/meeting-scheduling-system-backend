package com.meeting.schedule_a_meeting.controllers.users;

import com.meeting.schedule_a_meeting.dto.request.users.CreateMeetingRequest;
import com.meeting.schedule_a_meeting.dto.request.users.MeetingRoomScheduleRequest;
import com.meeting.schedule_a_meeting.dto.response.users.MeetingResponse;
import com.meeting.schedule_a_meeting.dto.response.users.TimeSlot;
import com.meeting.schedule_a_meeting.service.users.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/create")
    public MeetingResponse createMeeting(@RequestBody CreateMeetingRequest request,
                                         @RequestHeader("createdBy") String createdBy) {
        return meetingService.createMeeting(request, createdBy);
    }

    @PostMapping("/schedule")
    public List<TimeSlot> getMeetingRoomSchedule(@RequestBody MeetingRoomScheduleRequest request) {
        return meetingService.getMeetingRoomSchedule(request);
    }
    @DeleteMapping("/cancel/{id}")
    public String cancelMeeting(@PathVariable Long id,
                                @RequestHeader("createdBy") String createdBy) {
        meetingService.cancelMeeting(id, createdBy);
        return "The conversation was successfully canceled.!";
    }

}


