
package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.dto.response.users.meeting.MeetingReminderDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendReminderToUser(String email, MeetingReminderDTO dto) {
//        messagingTemplate.convertAndSendToUser(email, "/queue/reminders", dto);
    }
}
