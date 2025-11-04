// com.meeting.schedule_a_meeting.service.EmailService.java

package com.meeting.schedule_a_meeting.service.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String verifyLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Confirm your account !");
        message.setText("Click the following link to activate your account:\n\n" + verifyLink);
        mailSender.send(message);
    }
}