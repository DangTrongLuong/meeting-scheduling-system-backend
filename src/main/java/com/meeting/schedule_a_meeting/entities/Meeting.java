package com.meeting.schedule_a_meeting.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private MeetingRoom room;

    @ElementCollection
    @CollectionTable(name = "meeting_invitations", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "email")
    private List<String> invitedEmails;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name= "status",nullable = false)
    private String status = "ACTIVE"; // ACTIVE, CANCELLED
}
