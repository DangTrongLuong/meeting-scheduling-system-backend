package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.dto.request.users.CreateMeetingRequest;
import com.meeting.schedule_a_meeting.dto.request.users.MeetingRoomScheduleRequest;
import com.meeting.schedule_a_meeting.dto.response.users.MeetingResponse;
import com.meeting.schedule_a_meeting.dto.response.users.TimeSlot;
import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.repositories.MeetingRepository;
import com.meeting.schedule_a_meeting.repositories.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingRoomRepository meetingRoomRepository;

    private static final LocalTime START_OF_DAY = LocalTime.of(8, 0);
    private static final LocalTime END_OF_DAY = LocalTime.of(18, 0);

    public MeetingResponse createMeeting(CreateMeetingRequest request, String createdBy) {
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        MeetingRoom meetingRoom = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        List<Meeting> conflicts = meetingRepository
                .findByRoom_IdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                        request.getRoomId(), request.getEndTime(), request.getStartTime());

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Room is already booked for this time slot");
        }

        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .room(meetingRoom)
                .createdBy(createdBy)
                .invitedEmails(request.getInvitedEmails() != null ? request.getInvitedEmails() : Collections.emptyList())
                .build();

        Meeting saved = meetingRepository.save(meeting);

        return MeetingResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .roomName(saved.getRoom().getName())
                .createdBy(saved.getCreatedBy())
                .invitedEmails(saved.getInvitedEmails())
                .build();
    }

    public List<TimeSlot> getMeetingRoomSchedule(MeetingRoomScheduleRequest request) {
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().atTime(LocalTime.MAX);

        List<Meeting> bookedMeetings = meetingRepository.findByRoom_IdAndStartTimeBetweenOrderByStartTimeAsc(
                request.getRoomId(), startDateTime, endDateTime);

        List<TimeSlot> schedule = new ArrayList<>();
        LocalDateTime currentTime = startDateTime;

        while (currentTime.toLocalDate().isBefore(request.getEndDate().plusDays(1))) {
            LocalDateTime dayStart = currentTime.toLocalDate().atTime(START_OF_DAY);
            LocalDateTime dayEnd = currentTime.toLocalDate().atTime(END_OF_DAY);

            if (currentTime.isAfter(dayEnd)) {
                currentTime = currentTime.toLocalDate().plusDays(1).atStartOfDay();
                continue;
            }

            LocalDateTime lastSlotEnd = (currentTime.isBefore(dayStart)) ? dayStart : currentTime;

            for (Meeting meeting : bookedMeetings) {
                if (meeting.getStartTime().toLocalDate().isEqual(currentTime.toLocalDate()) &&
                        meeting.getStartTime().isAfter(lastSlotEnd.minusSeconds(1))) {

                    LocalDateTime actualMeetingStart = (meeting.getStartTime().isBefore(dayStart)) ? dayStart : meeting.getStartTime();
                    LocalDateTime actualMeetingEnd = (meeting.getEndTime().isAfter(dayEnd)) ? dayEnd : meeting.getEndTime();

                    if (lastSlotEnd.isBefore(actualMeetingStart)) {
                        schedule.add(new TimeSlot(lastSlotEnd, actualMeetingStart, false, "Available"));
                    }

                    if (actualMeetingStart.isBefore(actualMeetingEnd)) {
                        schedule.add(new TimeSlot(actualMeetingStart, actualMeetingEnd, true, meeting.getTitle()));
                    }

                    lastSlotEnd = actualMeetingEnd;
                }
            }

            if (lastSlotEnd.isBefore(dayEnd)) {
                schedule.add(new TimeSlot(lastSlotEnd, dayEnd, false, "Available"));
            }

            currentTime = currentTime.toLocalDate().plusDays(1).atStartOfDay();
        }

        return schedule;
    }
}