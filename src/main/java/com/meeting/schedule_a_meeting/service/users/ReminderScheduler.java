package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.entities.MeetingParticipant;
import com.meeting.schedule_a_meeting.repositories.meeting.MeetingParticipantRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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
    @Transactional
    public void sendMeetingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15);

        // Lấy tất cả người tham gia chưa gửi nhắc
        List<MeetingParticipant> participants =
                participantRepository.findParticipantsForReminderWithUserAndMeeting(now, reminderTime);



        Set<String> notifiedEmails = new HashSet<>();

        for (MeetingParticipant mp : participants) {
            String email = mp.getUser().getEmail();
            if (notifiedEmails.add(email)) {
                emailService.sendReminderEmail(email, mp.getMeeting());
            }

            String creatorEmail = mp.getMeeting().getCreator().getEmail();
            if (notifiedEmails.add(creatorEmail)) {
                emailService.sendReminderEmail(creatorEmail, mp.getMeeting());
            }

            mp.setReminderSent(true);
            participantRepository.save(mp);
        }

    }
}
