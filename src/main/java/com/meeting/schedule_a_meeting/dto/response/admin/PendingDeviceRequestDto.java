package com.meeting.schedule_a_meeting.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingDeviceRequestDto {
    private String deviceId;
    private String deviceName;
    private int totalRequestedQuantity; // Tổng số lượng đã được mượn thêm
    private int requestCount; // Số cuộc họp đã mượn thiết bị này
    private String roomId;
    private String roomName;
}
