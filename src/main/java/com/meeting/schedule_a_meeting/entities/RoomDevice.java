package com.meeting.schedule_a_meeting.entities;

import jakarta.persistence.*;
import lombok.*;
import com.meeting.schedule_a_meeting.enums.RoomDeviceStatus;
import com.meeting.schedule_a_meeting.util.IdGenerator;

@Entity
@Table(name = "room_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDevice {

    @Id
    @Column(length = 8, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private MeetingRoom meetingRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomDeviceStatus status = RoomDeviceStatus.IN_USE;

    @PrePersist
    private void generateId() {
        this.id = IdGenerator.generate("RD");
    }
}