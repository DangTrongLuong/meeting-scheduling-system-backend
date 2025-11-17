package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.dto.request.users.InviteUsersRequest;
import com.meeting.schedule_a_meeting.dto.response.users.InviteUsersResponse;
import com.meeting.schedule_a_meeting.entities.Meeting;
import com.meeting.schedule_a_meeting.repositories.MeetingRepository;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    // Mời người dùng vào cuộc họp
    public InviteUsersResponse inviteUsers(Long meetingId, InviteUsersRequest request, String currentUser) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc họp không tồn tại"));

        // Chỉ người tạo mới được mời
        if (!meeting.getCreatedBy().equals(currentUser)) {
            throw new IllegalArgumentException("Bạn không có quyền mời người khác cho cuộc họp này");
        }

        // Thêm email mới (không trùng)
        List<String> invitedEmails = new ArrayList<>(meeting.getInvitedEmails());
        for (String email : request.getEmails()) {
            if (!invitedEmails.contains(email)) {
                invitedEmails.add(email);
            }
        }
        meeting.setInvitedEmails(invitedEmails);
        meetingRepository.save(meeting);

        return InviteUsersResponse.builder()
                .meetingId(meeting.getId())
                .invitedEmails(invitedEmails)
                .build();
    }

    // Gợi ý email khi người dùng nhập
    public List<String> suggestEmails(String query) {
        return userRepository.searchEmailByQuery(query);
    }

    public String confirmInvitation(Long meetingId, String email) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));

        if (meeting.getInvitationStatus() != null && meeting.getInvitationStatus().containsKey(email)) {
            meeting.getInvitationStatus().put(email, "ACCEPTED");
            meetingRepository.save(meeting);
            return "Invitation confirmed successfully!";
        }
        return "Email not invited to this meeting.";
    }
}
