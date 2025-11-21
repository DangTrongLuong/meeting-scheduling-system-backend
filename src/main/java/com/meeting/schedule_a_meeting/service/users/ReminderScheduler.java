package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;


@Component
public class ReminderScheduler {

    private final MeetingRepository meetingRepository;
    private final EmailService emailService;

    public ReminderScheduler(MeetingRepository meetingRepository, EmailService emailService) {
        this.meetingRepository = meetingRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 60000) // chạy mỗi phút
    public void sendMeetingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15);

        List<Meeting> meetings = meetingRepository.findUpcomingMeetingsForReminder(now, reminderTime);


        for (Meeting meeting : meetings) {
            meeting.getParticipants().forEach(participant -> {
                emailService.sendReminderEmail(participant.getUser().getEmail(), meeting);
            });
        }

    }
}
