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
    private Long id;

    private String title;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String room;

    @ElementCollection
    @CollectionTable(name = "meeting_invitations", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "email")
    private List<String> invitedEmails;

    private String createdBy;
}
