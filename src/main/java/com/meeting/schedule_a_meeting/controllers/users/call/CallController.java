package com.meeting.schedule_a_meeting.controllers.users.call;

import java.util.List;
import java.util.UUID;

import com.meeting.schedule_a_meeting.dto.request.users.call.InitiateCallRequest;
import com.meeting.schedule_a_meeting.dto.response.users.call.CallSessionResponse;
import com.meeting.schedule_a_meeting.dto.response.users.call.TodayMeetingsResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.ApiResponse;
import com.meeting.schedule_a_meeting.service.CallService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
@Slf4j
public class CallController {

    private final CallService callService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<CallSessionResponse>> initiateCall(
            @RequestBody InitiateCallRequest request,
            @RequestHeader("userId") UUID userId) {
        log.info("Initiating call for meeting: {} by user: {}", request.getMeetingId(), userId);

        CallSessionResponse response = callService.initiateCall(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Call initiated successfully", response));
    }

    @GetMapping("/active/{meetingId}")
    public ResponseEntity<ApiResponse<CallSessionResponse>> getActiveCall(@PathVariable String meetingId) {
        log.info("Getting active call for meeting: {}", meetingId);

        CallSessionResponse response = callService.getActiveCall(meetingId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<TodayMeetingsResponse>>> getTodayMeetings(
            @RequestHeader("userId") UUID userId) {
        log.info("Getting today's meetings for user: {}", userId);

        List<TodayMeetingsResponse> response = callService.getTodayMeetings(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{callId}/accept")
    public ResponseEntity<ApiResponse<String>> acceptCall(
            @PathVariable UUID callId,
            @RequestHeader("userId") UUID userId) {
        log.info("User {} accepting call {}", userId, callId);

        callService.acceptCall(callId, userId);

        return ResponseEntity.ok(ApiResponse.success("Call accepted"));
    }

    @PatchMapping("/{callId}/decline")
    public ResponseEntity<ApiResponse<String>> declineCall(
            @PathVariable UUID callId,
            @RequestHeader("userId") UUID userId) {
        log.info("User {} declining call {}", userId, callId);

        callService.declineCall(callId, userId);

        return ResponseEntity.ok(ApiResponse.success("Call declined"));
    }

    @PatchMapping("/{callId}/end")
    public ResponseEntity<ApiResponse<String>> endCall(@PathVariable UUID callId) {
        log.info("Ending call: {}", callId);

        callService.endCall(callId);

        return ResponseEntity.ok(ApiResponse.success("Call ended"));
    }

    @PatchMapping("/{callId}/mute")
    public ResponseEntity<ApiResponse<String>> muteCall(
            @PathVariable UUID callId,
            @RequestHeader("userId") UUID userId,
            @RequestParam boolean mute) {
        log.info("User {} muting call {}: {}", userId, callId, mute);

        callService.toggleMute(callId, userId, mute);

        return ResponseEntity.ok(ApiResponse.success("Mute status updated"));
    }

    @PatchMapping("/{callId}/video")
    public ResponseEntity<ApiResponse<String>> toggleVideo(
            @PathVariable UUID callId,
            @RequestHeader("userId") UUID userId,
            @RequestParam boolean enable) {
        log.info("User {} video call {}: {}", userId, callId, enable);

        callService.toggleVideo(callId, userId, enable);

        return ResponseEntity.ok(ApiResponse.success("Video status updated"));
    }
}