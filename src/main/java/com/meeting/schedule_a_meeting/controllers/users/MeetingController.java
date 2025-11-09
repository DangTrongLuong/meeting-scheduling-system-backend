package com.meeting.schedule_a_meeting.controllers.users;

import com.meeting.schedule_a_meeting.dto.request.users.CreateMeetingRequest;
import com.meeting.schedule_a_meeting.dto.response.users.MeetingResponse;
import com.meeting.schedule_a_meeting.service.users.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<MeetingResponse> createMeeting(@RequestBody CreateMeetingRequest request) {
        // Giả lập user đang đăng nhập (sau này có thể lấy từ SecurityContext)
        String createdBy = "current.user@example.com";

        MeetingResponse response = meetingService.createMeeting(request, createdBy);
        return ResponseEntity.ok(response);
    }
}
