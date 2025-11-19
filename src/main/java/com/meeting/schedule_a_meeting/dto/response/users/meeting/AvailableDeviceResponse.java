package com.meeting.schedule_a_meeting.dto.response.users.meetting;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailableDeviceResponse {
    private String id;
    private String name;
    private String imagePath;
    private int totalQuantity;
    private int availableQuantity;
    private boolean alreadyAssignedToRoom;
    private int quantityInRoom;
}
