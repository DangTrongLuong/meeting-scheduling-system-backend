package com.meeting.schedule_a_meeting.entities;

import jakarta.persistence.*;
import lombok.*;
import com.meeting.schedule_a_meeting.enums.ParticipantRole;
import com.meeting.schedule_a_meeting.enums.ParticipantStatus;
import com.meeting.schedule_a_meeting.util.IdGenerator;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meeting_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "user_id"}),
        indexes = {
                @Index(name = "idx_participant_user", columnList = "user_id, meeting_id"),
                @Index(name = "idx_participant_meeting", columnList = "meeting_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingParticipant {

    @Id
    @Column(length = 8, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ParticipantRole role = ParticipantRole.REQUIRED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ParticipantStatus status = ParticipantStatus.PENDING;

    @CreationTimestamp
    @Column(name = "invited_at", nullable = false, updatable = false)
    private LocalDateTime invitedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "is_notified", nullable = false)
    @Builder.Default
    private boolean notified = false;

    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private boolean reminderSent = false;

    @PrePersist
    private void generateId() {
        this.id = IdGenerator.generate("MP");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetingParticipant)) return false;
        MeetingParticipant that = (MeetingParticipant) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}