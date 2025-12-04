package com.meeting.schedule_a_meeting.controllers.users;

import com.meeting.schedule_a_meeting.dto.response.users.GoogleCalendarStatusResponse;
import com.meeting.schedule_a_meeting.dto.response.users.GoogleCalendarSyncResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.*;
import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingRepository;
import com.meeting.schedule_a_meeting.service.users.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/google-calendar")
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final MeetingRepository meetingRepository;

    /**
     * Bước 1: User click "Connect Google Calendar"
     * Endpoint này trả về URL để redirect user sang Google OAuth
     */
    @GetMapping("/connect")
    public void connectGoogleCalendar(
            @RequestParam("userId") String userIdStr,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {

        log.info("User {} requesting Google Calendar connection", userIdStr);

        try {
            UUID userId = UUID.fromString(userIdStr);
            String authUrl = googleCalendarService.getAuthorizationUrl(userId);

            // Redirect trực tiếp sang Google OAuth
            response.sendRedirect(authUrl);

        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format: {}", userIdStr);
            response.sendRedirect("http://localhost:5173/user/meeting-schedule?error=invalid_user");
        } catch (Exception e) {
            log.error("Error generating auth URL", e);
            response.sendRedirect("http://localhost:5173/user/meeting-schedule?error=connection_failed");
        }
    }

    /**
     * Bước 2: Google redirect về đây sau khi user authorize
     * Callback endpoint để nhận authorization code
     */
    @GetMapping("/callback")
    public void handleGoogleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String userIdStr,
            jakarta.servlet.http.HttpServletResponse response) {

        log.info("========================================");
        log.info("🔵 GOOGLE CALLBACK RECEIVED");
        log.info("State (userId): {}", userIdStr);
        log.info("========================================");

        try {
            UUID userId = UUID.fromString(userIdStr);

            log.info("Parsing userId successful: {}", userId);
            log.info("Calling handleCallback service...");

            googleCalendarService.handleCallback(code, userId);

            log.info("handleCallback completed successfully");

            // TỰ ĐỘNG SYNC TẤT CẢ MEETINGS NGAY SAU KHI CONNECT
            log.info("Auto-syncing all meetings to Google Calendar...");
            try {
                GoogleCalendarSyncResponse syncResponse = googleCalendarService.syncAllMeetingsToGoogle(userId);
                log.info("Auto-sync completed: {} meetings synced", syncResponse.getSyncedCount());
            } catch (Exception syncError) {
                log.warn("Auto-sync failed, but connection is successful", syncError);
                // Không throw error, vẫn redirect về frontend
            }

            log.info("🔀 Redirecting to frontend with success...");

            response.sendRedirect("http://localhost:5173/user/meeting-schedule?google_calendar_connected=true");

        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format: {}", userIdStr, e);
            try {
                response.sendRedirect("http://localhost:5173/user/meeting-schedule?google_calendar_connected=false&error=invalid_user");
            } catch (Exception ex) {
                log.error("Error redirecting", ex);
            }
        } catch (Exception e) {
            log.error("CRITICAL ERROR in Google callback", e);
            e.printStackTrace();

            try {
                response.sendRedirect("http://localhost:5173/user/meeting-schedule?google_calendar_connected=false&error=callback_failed");
            } catch (Exception ex) {
                log.error("Error redirecting", ex);
            }
        }
    }

    /**
     * Kiểm tra user đã connect Google Calendar chưa
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<GoogleCalendarStatusResponse>> getConnectionStatus(
            @RequestHeader("userId") UUID userId) {

        log.info("Checking Google Calendar connection status for user: {}", userId);

        GoogleCalendarStatusResponse status = googleCalendarService.getConnectionStatus(userId);

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Đồng bộ một meeting cụ thể lên Google Calendar
     */
    @PostMapping("/sync-meeting/{meetingId}")
    public ResponseEntity<ApiResponse<String>> syncMeeting(
            @PathVariable String meetingId,
            @RequestHeader("userId") UUID userId) {

        log.info("Syncing meeting {} to Google Calendar for user {}", meetingId, userId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorStatus.MEETING_NOT_FOUND));

        // Check if user is creator
        if (!meeting.getCreator().getId().equals(userId)) {
            throw new AppException(ErrorStatus.FORBIDDEN, "Only meeting creator can sync to Google Calendar");
        }

        googleCalendarService.syncMeetingToGoogle(meeting, userId);

        return ResponseEntity.ok(ApiResponse.success("Meeting synced to Google Calendar successfully"));
    }

    /**
     * Đồng bộ TẤT CẢ meetings của user lên Google Calendar
     */
    @PostMapping("/sync-all")
    public ResponseEntity<ApiResponse<GoogleCalendarSyncResponse>> syncAllMeetings(
            @RequestHeader("userId") UUID userId) {

        log.info("Syncing all meetings to Google Calendar for user: {}", userId);

        GoogleCalendarSyncResponse syncResponse = googleCalendarService.syncAllMeetingsToGoogle(userId);

        return ResponseEntity.ok(ApiResponse.success(syncResponse));
    }

    /**
     * Ngắt kết nối Google Calendar
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<ApiResponse<String>> disconnectGoogleCalendar(
            @RequestHeader("userId") UUID userId) {

        log.info("Disconnecting Google Calendar for user: {}", userId);

        googleCalendarService.disconnect(userId);

        return ResponseEntity.ok(ApiResponse.success("Google Calendar disconnected successfully"));
    }
}