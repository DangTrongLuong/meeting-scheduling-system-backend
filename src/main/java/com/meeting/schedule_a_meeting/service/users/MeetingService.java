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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingRoomRepository meetingRoomRepository;

    // Tạo cuộc họp
    public MeetingResponse createMeeting(CreateMeetingRequest request, String createdBy) {
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Không tìm thấy phòng họp với ID: " + request.getRoomId()));
        // Kiểm tra trùng phòng
        List<Meeting> conflicts = meetingRepository
                .findByRoomAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                        room, request.getEndTime(), request.getStartTime());

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Phòng họp đã được đặt trong khung giờ này");
        }

        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .room(room)
                .status(request.getStatus())
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
                .status(request.getStatus())
                .invitedEmails(saved.getInvitedEmails())
                .build();
    }

    public List<TimeSlot> getMeetingRoomSchedule(MeetingRoomScheduleRequest request) {
        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng họp"));

        LocalDateTime dayStart = request.getStartDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Meeting> bookedMeetings = meetingRepository
                .findByRoomAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(room, dayStart, dayEnd);

        List<TimeSlot> slots = new ArrayList<>();
        LocalDateTime slotStart = dayStart.withHour(0).withMinute(0);

        while (slotStart.isBefore(dayEnd)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(30);

            final LocalDateTime currentStart = slotStart;
            final LocalDateTime currentEnd = slotEnd;

            boolean isBooked = bookedMeetings.stream()
                    .anyMatch(m -> !currentEnd.isBefore(m.getStartTime()) && !currentStart.isAfter(m.getEndTime()));

            slots.add(new TimeSlot(slotStart, slotEnd, !isBooked, ""));
            slotStart = slotEnd;
        }

        return slots;
    }
    public void cancelMeeting(Long meetingId, String createdBy) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc họp với ID: " + meetingId));

        // Kiểm tra quyền hủy
        if (!meeting.getCreatedBy().equals(createdBy)) {
            throw new IllegalArgumentException("Bạn không có quyền hủy cuộc họp này");
        }

        // Xóa cuộc họp
        meetingRepository.delete(meeting);
    }

    public MeetingResponse getMeetingDetail(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc họp"));
        return MeetingResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .roomName(meeting.getRoom().getName())
                .createdBy(meeting.getCreatedBy())
                .invitedEmails(meeting.getInvitedEmails())
                .status(meeting.getStatus())
                .build();
    }
}


