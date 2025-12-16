package com.meeting.schedule_a_meeting.enums;

public enum CallStatus {
    INITIATED,      // Call vừa được khởi tạo
    RINGING,        // Đang đổ chuông
    ANSWERED,       // Có người nhận
    DECLINED,       // Người nhận từ chối
    MISSED,         // Cuộc gọi bỏ lỡ
    ENDED           // Cuộc gọi kết thúc
}
