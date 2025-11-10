package com.meeting.schedule_a_meeting.dto.response.users;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
    // Thời gian bắt đầu của khoảng thời gian này
    private LocalDateTime start;

    // Thời gian kết thúc của khoảng thời gian này
    private LocalDateTime end;

    // true nếu khoảng thời gian này đã được đặt
    private boolean isBooked;

    // Tiêu đề (tên cuộc họp nếu đã đặt, hoặc "Available")
    private String title;
}