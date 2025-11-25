package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.entities.Meeting;
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

    public void sendResetCodeEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your Password Reset Code");
        message.setText(
                "Your password reset code is:\n\n" +
                        "   " + code + "\n\n" +
                        "This code will expire in 10 minutes.\n" +
                        "If you didn't request this, please ignore this email."
        );
        mailSender.send(message);
    }

    // ✅ Thêm phương thức gửi email nhắc nhở
    public void sendReminderEmail(String to, Meeting meeting) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Nhắc nhở cuộc họp: " + meeting.getTitle());
        message.setText(
                "Xin chào,\n\n" +
                        "Cuộc họp \"" + meeting.getTitle() + "\" sẽ bắt đầu sau 15 phút.\n\n" +
                        "Thời gian: " + meeting.getStartTime() + "\n" +
                        "Phòng họp: " + meeting.getMeetingRoom().getName() + "\n\n" +
                        "Vui lòng chuẩn bị để tham gia đúng giờ.\n\n" +
                        "Trân trọng."
        );
        mailSender.send(message);
    }

    public void sendEmailConfirmMeeting(String to, String title, String description,
                                        String startTime, String endTime,
                                        String roomName, String createdBy, String createdByEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Meeting Invitation");

        String emailContent = String.format(
                "Dear %s,\n\n" +
                        "You have been invited to the following meeting:\n\n" +
                        "Title: %s\n" +
                        "Description:%s\n" +
                        "Start Time: %s\n" +
                        "End Time: %s\n" +
                        "Room Name: %s\n" +
                        "Created By: %s - %s\n" +
                        "Please make sure to attend the meeting on time. If you have any questions, feel free to contact the meeting organizer.\n\n" +
                        "Best regards,\n%s",
                to, title, description, startTime, endTime, roomName, createdBy, createdByEmail, createdBy
        );

        message.setText(emailContent);
        mailSender.send(message);
    }
    public void sendEmailMeetingUpdated(
            String to,
            String meetingTitle,
            String description,
            String startTime,
            String endTime,
            String roomName,
            String updatedBy,
            String updatedByEmail
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Meeting Updated Notification");

        String emailContent = String.format(
                "Dear %s,\n\n" +
                        "The meeting you were invited to has been UPDATED.\n\n" +
                        "Updated Meeting Details:\n" +
                        "Title: %s\n" +
                        "Description: %s\n" +
                        "Start Time: %s\n" +
                        "End Time: %s\n" +
                        "Room Name: %s\n" +
                        "Updated By: %s (%s)\n\n" +
                        "Please check the meeting schedule again.\n\n" +
                        "Best regards,\n%s",
                to, meetingTitle, description, startTime, endTime, roomName,
                updatedBy, updatedByEmail, updatedBy
        );

        message.setText(emailContent);
        mailSender.send(message);
    }

}