package com.meeting.schedule_a_meeting.controllers.users;

import com.meeting.schedule_a_meeting.dto.response.users.SyncResultResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.MeetingResponse;
import com.meeting.schedule_a_meeting.service.users.GoogleCalendarService;
import com.meeting.schedule_a_meeting.service.users.MeetingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

    @RestController
    @RequestMapping("/api/calendar")
    @RequiredArgsConstructor
    @Slf4j
    public class CalendarSyncController {

        private final MeetingService meetingService;
        private final GoogleCalendarService googleCalendarService;


        @PostMapping("/sync-to-google")
        public ResponseEntity<SyncResultResponse> syncToGoogle(
                @RequestHeader("userId") String userIdHeader,
                @RequestHeader("Authorization") String authorizationHeader
        ) {

            UUID userId = UUID.fromString(userIdHeader);

            // Lấy accessToken từ header Authorization: Bearer <token>
            String accessToken = authorizationHeader.replace("Bearer ", "");


            if (accessToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new SyncResultResponse(0, 0, 0, List.of(), List.of()));
            }

            List<MeetingResponse> meetings = meetingService.getMyMeetings(userId);
            SyncResultResponse result = googleCalendarService.syncMeetingsToGoogle(meetings, accessToken);

            return ResponseEntity.ok(result);
        }


        @PostMapping("/disconnect-google")
        public ResponseEntity<?> disconnectGoogle(HttpSession session) {
            session.removeAttribute("accessToken");
            return ResponseEntity.ok("Đã bỏ đồng bộ Google Calendar.");
        }

        @GetMapping("/status")
        public ResponseEntity<?> checkStatus(HttpSession session) {
            boolean connected = session.getAttribute("accessToken") != null;
            return ResponseEntity.ok(Map.of("connected", connected));
        }



    }

