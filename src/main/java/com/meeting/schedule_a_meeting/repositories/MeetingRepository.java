package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // Kiểm tra trùng phòng: tìm các meeting có thời gian chồng lấn
    List<Meeting> findByRoomAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            MeetingRoom room, LocalDateTime endTime, LocalDateTime startTime);

    // Lấy tất cả các meeting của một phòng trong khoảng thời gian
    List<Meeting> findByRoomAndStartTimeBetween(
            MeetingRoom room, LocalDateTime start, LocalDateTime end);
}

