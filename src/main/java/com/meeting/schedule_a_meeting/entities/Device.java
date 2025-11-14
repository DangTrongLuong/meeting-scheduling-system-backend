package com.meeting.schedule_a_meeting.entities;

import jakarta.persistence.*;
import lombok.*;
import com.meeting.schedule_a_meeting.enums.DeviceStatus;
import com.meeting.schedule_a_meeting.util.IdGenerator;

@Entity
@Table(name = "devices", uniqueConstraints = @UniqueConstraint(columnNames = { "name", "status" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @Column(length = 8, updatable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "image_path")
    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity = 0;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity = 0;

    @PrePersist
    private void generateId() {
        this.id = IdGenerator.generate("DV");
        if (this.availableQuantity == 0)
            this.availableQuantity = this.totalQuantity;
    }
}