package com.meeting.schedule_a_meeting.controllers.users;

import com.meeting.schedule_a_meeting.dto.request.users.InviteUsersRequest;
import com.meeting.schedule_a_meeting.dto.response.users.InviteUsersResponse;
import com.meeting.schedule_a_meeting.service.users.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/meetings")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    // Endpoint mời người dùng
    @PostMapping("/{meetingId}/invite")
    public ResponseEntity<InviteUsersResponse> inviteUsers(
            @PathVariable Long meetingId,
            @RequestBody InviteUsersRequest request
    ) {
        String currentUser = "current.user@example.com"; // giả lập user đăng nhập
        InviteUsersResponse response = invitationService.inviteUsers(meetingId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    // Endpoint gợi ý email
    @GetMapping("/invite/suggest")
    public ResponseEntity<List<String>> suggestEmails(@RequestParam String query) {
        List<String> suggestions = invitationService.suggestEmails(query);
        return ResponseEntity.ok(suggestions);
    }
}
