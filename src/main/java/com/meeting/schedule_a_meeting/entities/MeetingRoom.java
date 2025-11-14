package com.meeting.schedule_a_meeting.entities;

import jakarta.persistence.*;
import lombok.*;
import com.meeting.schedule_a_meeting.util.IdGenerator;

@Entity
@Table(name = "meeting_rooms", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingRoom {

    @Id
    @Column(length = 8, updatable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Integer capacity;

    @PrePersist
    private void generateId() {
        this.id = IdGenerator.generate("RM");
    }
}