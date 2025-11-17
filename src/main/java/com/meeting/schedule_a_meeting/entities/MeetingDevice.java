package com.meeting.schedule_a_meeting.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import com.meeting.schedule_a_meeting.enums.MeetingDeviceStatus;
import com.meeting.schedule_a_meeting.util.IdGenerator;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_devices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "device_id"}),
        indexes = {
                @Index(name = "idx_meeting_device", columnList = "device_id, meeting_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingDevice {

    @Id
    @Column(length = 8, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MeetingDeviceStatus status = MeetingDeviceStatus.RESERVED;

    @CreationTimestamp
    @Column(name = "reserved_at", nullable = false, updatable = false)
    private LocalDateTime reservedAt;

    @Column(length = 500)
    private String notes;

    @PrePersist
    private void generateId() {
        this.id = IdGenerator.generate("MD");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetingDevice)) return false;
        MeetingDevice that = (MeetingDevice) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}