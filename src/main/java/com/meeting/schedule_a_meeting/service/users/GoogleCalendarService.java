package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.dto.response.users.SyncResultResponse;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.MeetingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GoogleCalendarService {

    private static final String GOOGLE_CALENDAR_API_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";

    public SyncResultResponse syncMeetingsToGoogle(List<MeetingResponse> meetings, String accessToken) {
        log.info("=== STARTING GOOGLE CALENDAR SYNC ===");
        log.info("Total meetings to sync: {}", meetings != null ? meetings.size() : 0);
        log.info("Access token present: {}", accessToken != null && !accessToken.isEmpty());

        if (meetings == null || meetings.isEmpty()) {
            log.warn("No meetings to sync");
            return new SyncResultResponse(0, 0, 0, new ArrayList<>(), new ArrayList<>());
        }

        RestTemplate restTemplate = new RestTemplate();
        int syncedCount = 0;
        int skippedCount = 0;
        List<String> skippedTitles = new ArrayList<>();
        List<String> failedTitles = new ArrayList<>();

        for (int i = 0; i < meetings.size(); i++) {
            MeetingResponse meeting = meetings.get(i);
            log.info("--- Processing meeting {}/{} ---", i + 1, meetings.size());

            try {
                // Validate meeting object
                if (meeting == null) {
                    log.warn("Meeting at index {} is null, skipping", i);
                    skippedCount++;
                    skippedTitles.add("Null Meeting");
                    continue;
                }

                log.info("Meeting ID: {}", meeting.getId());
                log.info("Meeting Title: {}", meeting.getTitle());
                log.info("Meeting Start Time: {}", meeting.getStartTime());
                log.info("Meeting End Time: {}", meeting.getEndTime());

                // Get title with fallback
                String title = (meeting.getTitle() != null && !meeting.getTitle().isBlank())
                        ? meeting.getTitle()
                        : "Untitled Meeting";

                // Validate start and end time
                if (meeting.getStartTime() == null || meeting.getEndTime() == null) {
                    log.warn("Meeting '{}' has null start or end time, skipping", title);
                    skippedCount++;
                    skippedTitles.add(title);
                    continue;
                }

                // Create Google Calendar event
                Map<String, Object> event = new HashMap<>();
                event.put("summary", title);
                event.put("description", buildDescription(meeting));
                event.put("start", Map.of(
                        "dateTime", meeting.getStartTime().toString(),
                        "timeZone", "Asia/Ho_Chi_Minh"
                ));
                event.put("end", Map.of(
                        "dateTime", meeting.getEndTime().toString(),
                        "timeZone", "Asia/Ho_Chi_Minh"
                ));

                // Add attendees if available
//                if (meeting.getInvitedEmails() != null && !meeting.getInvitedEmails().isEmpty()) {
//                    List<Map<String, String>> attendees = new ArrayList<>();
//                    for (String email : meeting.getInvitedEmails()) {
//                        if (email != null && !email.isBlank()) {
//                            attendees.add(Map.of("email", email));
//                        }
//                    }
//                    if (!attendees.isEmpty()) {
//                        event.put("attendees", attendees);
//                    }
//                }

                log.debug("Event payload: {}", event);

                // Prepare HTTP request
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(accessToken);
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(event, headers);

                // Send to Google Calendar API
                log.info("Sending request to Google Calendar API for meeting '{}'", title);
                restTemplate.postForEntity(GOOGLE_CALENDAR_API_URL, request, String.class);

                syncedCount++;
                log.info("✓ Successfully synced meeting '{}'", title);

            } catch (Exception e) {
                String meetingTitle = (meeting != null && meeting.getTitle() != null)
                        ? meeting.getTitle()
                        : "Unknown Meeting";
                failedTitles.add(meetingTitle);
                log.error("✗ Failed to sync meeting '{}': {}", meetingTitle, e.getMessage());
                log.debug("Full error details: ", e);
            }
        }

        SyncResultResponse result = new SyncResultResponse(
                meetings.size(),
                syncedCount,
                skippedCount,
                skippedTitles,
                failedTitles
        );

        log.info("=== SYNC COMPLETED ===");
        log.info("Total: {}, Synced: {}, Skipped: {}, Failed: {}",
                result.getTotalMeetings(),
                result.getSyncedCount(),
                result.getSkippedCount(),
                result.getFailedTitles().size()
        );

        return result;
    }

    private String buildDescription(MeetingResponse meeting) {
        StringBuilder description = new StringBuilder();

//        if (meeting.getRoom() != null && !meeting.getRoom().isBlank()) {
//            description.append("Room: ").append(meeting.getRoom()).append("\n");
//        }
//
//        if (meeting.getCreatedBy() != null && !meeting.getCreatedBy().isBlank()) {
//            description.append("Created by: ").append(meeting.getCreatedBy()).append("\n");
//        }
//
//        if (meeting.getStatus() != null && !meeting.getStatus().isBlank()) {
//            description.append("Status: ").append(meeting.getStatus()).append("\n");
//        }

        if (description.length() == 0) {
            return "Meeting scheduled via Meeting Scheduling System";
        }

        return description.toString().trim();
    }
}