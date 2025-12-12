package com.meeting.schedule_a_meeting.service.users;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.meeting.schedule_a_meeting.config.GoogleCalendarConfig;
import com.meeting.schedule_a_meeting.dto.response.users.GoogleCalendarStatusResponse;
import com.meeting.schedule_a_meeting.dto.response.users.GoogleCalendarSyncResponse;
import com.meeting.schedule_a_meeting.entities.GoogleCalendarConnection;
import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.entities.MeetingParticipant;
import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.enums.MeetingStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.repositories.GoogleCalendarConnectionRepository;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {

    private final GoogleCalendarConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final GoogleCalendarConfig googleConfig;
    private final GoogleAuthorizationCodeFlow googleAuthFlow;
    private final NetHttpTransport httpTransport;
    private final JsonFactory jsonFactory;
    private static final String TIME_ZONE = "Asia/Ho_Chi_Minh"; // GMT+7
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * Tạo URL để user authorize với Google
     */
    public String getAuthorizationUrl(UUID userId) {
        log.info("Generating Google authorization URL for user: {}", userId);

        String authUrl = googleAuthFlow.newAuthorizationUrl()
                .setRedirectUri(googleConfig.getRedirectUri())
                .setState(userId.toString())
                .build();

        log.info("Authorization URL generated: {}", authUrl);
        return authUrl;
    }

    /**
     * Xử lý callback từ Google sau khi user authorize
     */
    @Transactional
    public void handleCallback(String code, UUID userId) {
        log.info("========================================");
        log.info("🔵 Handling Google Calendar callback for user: {}", userId);
        log.info("========================================");

        try {
            log.info("🔄 Exchanging authorization code for tokens...");

            GoogleTokenResponse tokenResponse = googleAuthFlow.newTokenRequest(code)
                    .setRedirectUri(googleConfig.getRedirectUri())
                    .execute();

            log.info("✅ Tokens received successfully");

            log.info("🔄 Finding user in database...");

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorStatus.USER_NOT_FOUND));

            log.info("✅ User found: {} ({})", user.getName(), user.getEmail());

            log.info("🔄 Deactivating old connections...");

            connectionRepository.findByUserIdAndIsActiveTrue(userId)
                    .ifPresent(existing -> {
                        existing.setActive(false);
                        connectionRepository.save(existing);
                        log.info("✅ Old connection deactivated");
                    });

            log.info("🔄 Creating new connection...");

            GoogleCalendarConnection connection = GoogleCalendarConnection.builder()
                    .user(user)
                    .googleEmail(user.getEmail()) // ← Dùng email từ bảng users
                    .accessToken(tokenResponse.getAccessToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .tokenExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()))
                    .isActive(true)
                    .connectedAt(LocalDateTime.now())
                    .build();

            log.info("🔄 Saving connection to database...");

            GoogleCalendarConnection savedConnection = connectionRepository.save(connection);

            log.info("✅ Connection saved successfully!");
            log.info("Connection ID: {}", savedConnection.getId());
            log.info("Google Email: {}", savedConnection.getGoogleEmail());
            log.info("========================================");
            log.info("✅✅✅ CALLBACK COMPLETED SUCCESSFULLY ✅✅✅");
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ Error in callback", e);
            throw new AppException(ErrorStatus.INTERNAL_SERVER_ERROR,
                    "Failed to connect Google Calendar: " + e.getMessage());
        }
    }

    /**
     * Đồng bộ một meeting lên Google Calendar
     */
    @Transactional
    public void syncMeetingToGoogle(Meeting meeting, UUID userId) {
        log.info("Syncing meeting {} to Google Calendar for user {}", meeting.getId(), userId);

        GoogleCalendarConnection connection = getActiveConnection(userId);
        Calendar calendarService = getCalendarService(connection);

        try {
            Event event = createGoogleEvent(meeting);

            if (meeting.getGoogleEventId() != null) {
                // Update existing event
                calendarService.events()
                        .update("primary", meeting.getGoogleEventId(), event)
                        .execute();
                log.info("Updated Google Calendar event: {}", meeting.getGoogleEventId());
            } else {
                // Create new event
                Event createdEvent = calendarService.events()
                        .insert("primary", event)
                        .execute();

                meeting.setGoogleEventId(createdEvent.getId());
                log.info("Created Google Calendar event: {}", createdEvent.getId());
            }

            meeting.setLastSyncedAt(LocalDateTime.now());
            connection.setLastSyncedAt(LocalDateTime.now());

            meetingRepository.save(meeting);
            connectionRepository.save(connection);

        } catch (IOException e) {
            log.error("Error syncing meeting to Google Calendar", e);
            throw new AppException(ErrorStatus.INTERNAL_SERVER_ERROR, "Failed to sync meeting");
        }
    }

    /**
     * Đồng bộ tất cả meetings của user lên Google Calendar
     */
    @Transactional
    public GoogleCalendarSyncResponse syncAllMeetingsToGoogle(UUID userId) {
        log.info("Syncing all meetings to Google Calendar for user: {}", userId);

        GoogleCalendarConnection connection = getActiveConnection(userId);

        // Get all meetings where user is creator
        List<Meeting> meetings = meetingRepository.findByCreatorIdOrderByStartTimeDesc(userId);

        int syncedCount = 0;
        int failedCount = 0;

        for (Meeting meeting : meetings) {
            // Only sync SCHEDULED and PENDING_APPROVAL meetings
            if (meeting.getStatus() == MeetingStatus.SCHEDULED) {
                try {
                    syncMeetingToGoogle(meeting, userId);
                    syncedCount++;
                } catch (Exception e) {
                    log.error("Failed to sync meeting {}", meeting.getId(), e);
                    failedCount++;
                }
            }
        }

        connection.setLastSyncedAt(LocalDateTime.now());
        connectionRepository.save(connection);

        log.info("Sync completed: {} synced, {} failed", syncedCount, failedCount);

        return GoogleCalendarSyncResponse.builder()
                .syncedCount(syncedCount)
                .failedCount(failedCount)
                .message(syncedCount + " meetings synced successfully")
                .build();
    }

    /**
     * Xóa event từ Google Calendar
     */
    @Transactional
    public void deleteGoogleEvent(String eventId, String accessToken) {
        if (eventId == null || accessToken == null || accessToken.isEmpty()) {
            log.warn("Missing eventId or accessToken, skipping Google Calendar deletion");
            return;
        }
        try {
            Calendar calendar = getCalendarClient(accessToken);
            calendar.events().delete("primary", eventId).execute();
            log.info("✅ Google Calendar event deleted: {}", eventId);
        } catch (Exception e) {
            log.error("❌ Failed to delete Google Calendar event {}: {}", eventId, e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteGoogleEventIfExists(Meeting meeting, UUID userId) {
        if (meeting.getGoogleEventId() == null || meeting.getGoogleEventId().isBlank()) {
            log.info("Meeting {} has no Google Event ID -> nothing to delete on Google Calendar", meeting.getId());
            return;
        }

        try {
            GoogleCalendarConnection connection = getActiveConnection(userId);
            Calendar calendarService = getCalendarService(connection);

            calendarService.events()
                    .delete("primary", meeting.getGoogleEventId())
                    .execute();

            log.info("Successfully deleted Google Calendar event {} for meeting {}", meeting.getGoogleEventId(),
                    meeting.getId());

            // Xóa ID khỏi DB để tránh thử xóa lại lần sau
            meeting.setGoogleEventId(null);
            meeting.setLastSyncedAt(null);

        } catch (Exception e) {
            // Nếu event đã bị xóa thủ công trên Google rồi → Google trả 410 Gone hoặc 404
            if (e.getMessage() != null && (e.getMessage().contains("410") || e.getMessage().contains("404"))) {
                log.info("Google Event {} already deleted or not found (normal when user deleted manually)",
                        meeting.getGoogleEventId());
                meeting.setGoogleEventId(null); // vẫn dọn dẹp DB
            } else {
                log.warn("Failed to delete Google Calendar event {} for meeting {}", meeting.getGoogleEventId(),
                        meeting.getId(), e);
            }
        }
    }

    /**
     * Kiểm tra user đã kết nối Google Calendar chưa
     */
    public boolean isConnected(UUID userId) {
        return connectionRepository.existsByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Lấy trạng thái kết nối Google Calendar
     */
    public GoogleCalendarStatusResponse getConnectionStatus(UUID userId) {
        return connectionRepository.findByUserIdAndIsActiveTrue(userId)
                .map(conn -> GoogleCalendarStatusResponse.builder()
                        .connected(true)
                        .googleEmail(conn.getGoogleEmail())
                        .connectedAt(conn.getConnectedAt())
                        .lastSyncedAt(conn.getLastSyncedAt())
                        .build())
                .orElse(GoogleCalendarStatusResponse.builder()
                        .connected(false)
                        .build());
    }

    /**
     * Ngắt kết nối Google Calendar
     */
    @Transactional
    public void disconnect(UUID userId) {
        log.info("Disconnecting Google Calendar for user: {}", userId);

        connectionRepository.findByUserIdAndIsActiveTrue(userId)
                .ifPresent(connection -> {
                    connection.setActive(false);
                    connectionRepository.save(connection);
                    log.info("Google Calendar disconnected for user: {}", userId);
                });
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private GoogleCalendarConnection getActiveConnection(UUID userId) {
        GoogleCalendarConnection connection = connectionRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new AppException(ErrorStatus.FORBIDDEN,
                        "Google Calendar not connected. Please connect first."));

        // Check if token expired
        if (connection.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            refreshAccessToken(connection);
        }

        return connection;
    }

    private void refreshAccessToken(GoogleCalendarConnection connection) {
        log.info("Refreshing access token for user: {}", connection.getUser().getId());

        try {
            GoogleTokenResponse tokenResponse = new GoogleRefreshTokenRequest(
                    httpTransport,
                    jsonFactory,
                    connection.getRefreshToken(),
                    googleConfig.getClientId(),
                    googleConfig.getClientSecret()).execute();

            connection.setAccessToken(tokenResponse.getAccessToken());
            connection.setTokenExpiresAt(
                    LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));

            connectionRepository.save(connection);
            log.info("Access token refreshed successfully");

        } catch (IOException e) {
            log.error("Error refreshing access token", e);
            throw new AppException(ErrorStatus.INTERNAL_SERVER_ERROR,
                    "Failed to refresh Google Calendar access token");
        }
    }

    private Calendar getCalendarService(GoogleCalendarConnection connection) {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setClientSecrets(googleConfig.getClientId(), googleConfig.getClientSecret())
                .build()
                .setAccessToken(connection.getAccessToken())
                .setRefreshToken(connection.getRefreshToken());

        return new Calendar.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Meeting Scheduling System")
                .build();
    }

    private Event createGoogleEvent(Meeting meeting) {
        Event event = new Event()
                .setSummary(meeting.getTitle())
                .setDescription(meeting.getDescription())
                .setLocation(meeting.getMeetingRoom().getName());

        // Set start time
        EventDateTime start = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(
                        meeting.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                .setTimeZone("Asia/Ho_Chi_Minh");
        event.setStart(start);

        // Set end time
        EventDateTime end = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(
                        meeting.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                .setTimeZone("Asia/Ho_Chi_Minh");
        event.setEnd(end);

        // Add participants
        List<EventAttendee> attendees = new ArrayList<>();
        for (MeetingParticipant participant : meeting.getParticipants()) {
            EventAttendee attendee = new EventAttendee()
                    .setEmail(participant.getUser().getEmail());
            attendees.add(attendee);
        }
        event.setAttendees(attendees);

        // Set reminders
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(
                        new EventReminder().setMethod("email").setMinutes(30),
                        new EventReminder().setMethod("popup").setMinutes(10)));
        event.setReminders(reminders);

        return event;
    }

    private Event buildEvent(Meeting meeting) {
        Event event = new Event()
                .setSummary(meeting.getTitle())
                .setDescription(meeting.getDescription())
                .setLocation(meeting.getMeetingRoom() != null ? meeting.getMeetingRoom().getName() : "");

        EventDateTime start = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(
                        meeting.getStartTime().atZone(ZoneId.of(TIME_ZONE)).toInstant().toEpochMilli()))
                .setTimeZone(TIME_ZONE);
        event.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(
                        meeting.getEndTime().atZone(ZoneId.of(TIME_ZONE)).toInstant().toEpochMilli()))
                .setTimeZone(TIME_ZONE);
        event.setEnd(end);

        // ✅ Sửa các ký tự HTML-escaped thành Java thật
        List<EventAttendee> attendees = new ArrayList<>();
        for (MeetingParticipant participant : meeting.getParticipants()) {
            if (participant.getUser() != null && participant.getUser().getEmail() != null) {
                attendees.add(new EventAttendee().setEmail(participant.getUser().getEmail()));
            }
        }
        if (!attendees.isEmpty()) {
            event.setAttendees(attendees);
        }

        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(
                        new EventReminder().setMethod("email").setMinutes(30),
                        new EventReminder().setMethod("popup").setMinutes(10)));
        event.setReminders(reminders);

        return event;
    }

    private Calendar getCalendarClient(String accessToken) throws Exception {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        HttpRequestInitializer initializer = request -> {
            request.getHeaders().setAuthorization("Bearer " + accessToken);
        };

        return new Calendar.Builder(httpTransport, JSON_FACTORY, initializer)
                .setApplicationName("Meeting Scheduling System")
                .build();
    }
}
