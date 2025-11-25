package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.dto.response.users.meeting.MeetingReminderDTO;
import com.meeting.schedule_a_meeting.entities.MeetingParticipant;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingParticipantRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;


@Component
public class ReminderScheduler {

    private final MeetingParticipantRepository participantRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;


    public ReminderScheduler(MeetingParticipantRepository participantRepository,
                             EmailService emailService,
                             NotificationService notificationService) {
        this.participantRepository = participantRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }


    @Scheduled(fixedRate = 60000)
    public void sendMeetingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15);

        List<MeetingParticipant> participants =
                participantRepository.findParticipantsForReminder(now, reminderTime);

        for (MeetingParticipant participant : participants) {
            emailService.sendReminderEmail(participant.getUser().getEmail(), participant.getMeeting());

            MeetingReminderDTO dto = new MeetingReminderDTO(
                    participant.getMeeting().getTitle(),
                    participant.getMeeting().getStartTime(),
                    participant.getMeeting().getMeetingRoom().getName()
            );
            notificationService.sendReminderToUser(participant.getUser().getEmail(), dto);

            participant.setReminderSent(true);
            participantRepository.save(participant);
        }
    }

}
