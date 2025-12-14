package com.meeting.schedule_a_meeting.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.meeting.schedule_a_meeting.enums.MeetingStatus;
import com.meeting.schedule_a_meeting.util.IdGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "meetings", indexes = {
        @Index(name = "idx_meeting_room_time", columnList = "room_id, start_time, end_time, status"),
        @Index(name = "idx_meeting_creator", columnList = "creator_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {

    @Id
    @Column(length = 8, updatable = false)
    private String id;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    @NotNull(message = "Meeting room is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private MeetingRoom meetingRoom;

    @NotNull(message = "Creator is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Users creator;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "google_event_id", length = 255)
    private String googleEventId;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "is_repeat", nullable = false)
    @Builder.Default
    private boolean isRepeat = false;

    @Column(name = "repeat_group_id", length = 36)
    private String repeatGroupId;

    @Column(name = "repeat_days", length = 100)
    private String repeatDays;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MeetingParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference
    private List<MeetingDevice> devices = new ArrayList<>();

    @PrePersist
    private void generateId() {
        this.id = IdGenerator.generate("MT");
    }

    // Helper methods
    public void addParticipant(MeetingParticipant participant) {
        participants.add(participant);
        participant.setMeeting(this);
    }

    public void removeParticipant(MeetingParticipant participant) {
        participants.remove(participant);
        participant.setMeeting(null);
    }

    public void addDevice(MeetingDevice device) {
        devices.add(device);
        device.setMeeting(this);
    }

    public void removeDevice(MeetingDevice device) {
        devices.remove(device);
        device.setMeeting(null);
    }
}