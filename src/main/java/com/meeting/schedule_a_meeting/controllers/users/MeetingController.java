package com.meeting.schedule_a_meeting.controllers.users;



import com.meeting.schedule_a_meeting.dto.request.users.meetting.CreateMeetingRequest;
import com.meeting.schedule_a_meeting.dto.request.users.meetting.UpdateMeetingRequest;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.ApiResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.MeetingResponse;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.service.users.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingController {

    private final MeetingService meetingService;


    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
            @Valid @RequestBody CreateMeetingRequest request) {

        log.info("=== CREATE MEETING REQUEST ===");
        log.info("Request: {}", request);

      UUID userId = getCurrentUserId();

        log.info("Current User ID: {}", userId);

        MeetingResponse response = meetingService.createMeeting(request, userId);
        log.info("Meeting created successfully: {}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Meeting created successfully", response));
    }


    @GetMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeetingById(
            @PathVariable String meetingId) {

        UUID userId = getCurrentUserId();
        MeetingResponse response = meetingService.getMeetingById(meetingId, userId);

        return ResponseEntity.ok(
                ApiResponse.<MeetingResponse>builder()
                        .code(200)
                        .message("Meeting retrieved successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/my-meetings")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMyMeetings() {
        UUID userId = getCurrentUserId();
        List<MeetingResponse> response = meetingService.getMyMeetings(userId);

        return ResponseEntity.ok(
                ApiResponse.<List<MeetingResponse>>builder()
                        .code(200)
                        .message("Meetings retrieved successfully")
                        .data(response)
                        .build()
        );
    }


    @GetMapping("/created")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMyCreatedMeetings() {
        UUID userId = getCurrentUserId();
        List<MeetingResponse> response = meetingService.getMyCreatedMeetings(userId);

        return ResponseEntity.ok(
                ApiResponse.<List<MeetingResponse>>builder()
                        .code(200)
                        .message("Created meetings retrieved successfully")
                        .data(response)
                        .build()
        );
    }


    @GetMapping("/room/{roomId}/schedule")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getRoomSchedule(
            @PathVariable String roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<MeetingResponse> response = meetingService.getRoomSchedule(roomId, startDate, endDate);

        return ResponseEntity.ok(
                ApiResponse.<List<MeetingResponse>>builder()
                        .code(200)
                        .message("Room schedule retrieved successfully")
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingResponse>> updateMeeting(
            @PathVariable String meetingId,
            @Valid @RequestBody UpdateMeetingRequest request) {

        UUID userId = getCurrentUserId();
        MeetingResponse response = meetingService.updateMeeting(meetingId, request, userId);

        return ResponseEntity.ok(
                ApiResponse.<MeetingResponse>builder()
                        .code(200)
                        .message("Meeting updated successfully")
                        .data(response)
                        .build()
        );
    }


    @DeleteMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<Void>> cancelMeeting(
            @PathVariable String meetingId,
            @RequestParam(required = false) String reason) {

        UUID userId = getCurrentUserId();
        meetingService.cancelMeeting(meetingId, userId, reason);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(200)
                        .message("Meeting cancelled successfully")
                        .build()
        );
    }


    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authentication found");
            throw new AppException(ErrorStatus.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        log.debug("Principal type: {}, value: {}", principal.getClass().getName(), principal);

        if (principal instanceof String) {
            String principalStr = (String) principal;
            if ("anonymousUser".equals(principalStr)) {
                log.error("User is anonymous (not authenticated)");
                throw new AppException(ErrorStatus.UNAUTHORIZED, "Authentication required");
            }

            try {
                return UUID.fromString(principalStr);
            } catch (IllegalArgumentException e) {
                log.error("Invalid userId format: {}", principalStr);
                throw new AppException(ErrorStatus.UNAUTHORIZED, "Invalid user ID in token");
            }
        }

        log.error("Unexpected principal type: {}", principal.getClass().getName());
        throw new AppException(ErrorStatus.UNAUTHORIZED, "Invalid authentication");
    }

}


