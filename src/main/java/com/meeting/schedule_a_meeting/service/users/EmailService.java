package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.entities.Meeting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendVerificationEmail(String to, String verifyLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Confirm your account !");
        message.setText("Click the following link to activate your account:\n\n" + verifyLink);
        mailSender.send(message);
    }

    @Async
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

    @Async
    public void sendFirstLoginCodeEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("First Login Verification Code");
        message.setText(
                "Welcome! This is your first login.\n\n" +
                        "Your verification code is:\n\n" +
                        "   " + code + "\n\n" +
                        "This code will expire in 10 minutes.\n" +
                        "Please enter this code to continue setting up your account.\n\n" +
                        "If you didn't request this, please contact support immediately."
        );
        mailSender.send(message);
    }

    // ✅ Thêm phương thức gửi email nhắc nhở
    @Async
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

    @Async
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

    @Async
    public void sendCancelMeetingEmail(String to, Meeting meeting) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Thông báo hủy cuộc họp: " + meeting.getTitle());
        message.setText(
                "Xin chào,\n\n" +
                        "Cuộc họp \"" + meeting.getTitle() + "\" đã bị hủy.\n\n" +
                        "Thời gian: " + meeting.getStartTime() + " - " + meeting.getEndTime() + "\n" +
                        "Phòng họp: " + meeting.getMeetingRoom().getName() + "\n" +
                        "Lý do: " + (meeting.getCancellationReason() != null ? meeting.getCancellationReason() : "Không có thông tin") + "\n" +
                        "Thời điểm hủy: " + meeting.getCancelledAt() + "\n\n" +
                        "Liên hệ người tạo: " + meeting.getCreator().getEmail() + "\n\n" +
                        "Trân trọng."
        );
        mailSender.send(message);
    }

    @Async
    public void sendEmailMeetingUpdated(String to, String title, String description,
                                        String startTime, String endTime,
                                        String roomName, String updatedBy, String updatedByEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Meeting Updated: " + title);

        String emailContent = String.format(
                "Dear %s,\n\n" +
                        "The meeting has been updated:\n\n" +
                        "Title: %s\n" +
                        "Description: %s\n" +
                        "Start Time: %s\n" +
                        "End Time: %s\n" +
                        "Room: %s\n" +
                        "Updated By: %s (%s)\n\n" +
                        "Please check the updated details and attend on time.\n\n" +
                        "Best regards,\n%s",
                to, title, description, startTime, endTime, roomName, updatedBy, updatedByEmail, updatedBy
        );

        message.setText(emailContent);
        mailSender.send(message);
    }
}