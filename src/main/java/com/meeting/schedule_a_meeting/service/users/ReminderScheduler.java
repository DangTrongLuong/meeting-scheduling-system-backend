package com.meeting.schedule_a_meeting.service.users;

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

    public ReminderScheduler(MeetingParticipantRepository participantRepository,
                             EmailService emailService) {
        this.participantRepository = participantRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 60000) // chạy mỗi 1 phút
    public void sendMeetingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15);

        // Lấy tất cả người tham gia chưa gửi nhắc
        List<MeetingParticipant> participants =
                participantRepository.findParticipantsForReminder(now, reminderTime);

        for (MeetingParticipant participant : participants) {

            // Gửi email
            emailService.sendReminderEmail(participant.getUser().getEmail(), participant.getMeeting());

            // Đánh dấu đã gửi
            participant.setReminderSent(true);

            // Lưu lại vào DB
            participantRepository.save(participant);
        }
    }
}
