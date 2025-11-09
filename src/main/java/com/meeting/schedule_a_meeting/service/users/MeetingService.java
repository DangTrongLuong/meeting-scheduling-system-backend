package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.dto.request.users.CreateMeetingRequest;
import com.meeting.schedule_a_meeting.dto.response.users.MeetingResponse;
import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.repositories.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;

    public MeetingResponse createMeeting(CreateMeetingRequest request, String createdBy) {
        // Kiểm tra logic thời gian
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        // Kiểm tra trùng phòng
        List<Meeting> conflicts = meetingRepository
                .findByRoomAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                        request.getRoom(), request.getEndTime(), request.getStartTime());

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Phòng họp đã được đặt trong khung giờ này");
        }

        // Tạo mới meeting
        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .room(request.getRoom())
                .createdBy(createdBy)
                .invitedEmails(Collections.emptyList())
                .build();

        Meeting saved = meetingRepository.save(meeting);

        return MeetingResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .room(saved.getRoom())
                .createdBy(saved.getCreatedBy())
                .invitedEmails(saved.getInvitedEmails())
                .build();
    }
}
