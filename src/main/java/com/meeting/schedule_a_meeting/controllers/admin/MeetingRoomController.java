package com.meeting.schedule_a_meeting.controllers.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.MeetingRoomRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.MeetingRoomResponse;
import com.meeting.schedule_a_meeting.service.admin.MeetingRoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rooms")
public class MeetingRoomController {
    private final MeetingRoomService meetingRoomService;

    public MeetingRoomController(MeetingRoomService meetingRoomService) {
        this.meetingRoomService = meetingRoomService;
    }


    //them_phong
    @PostMapping
    public ResponseEntity<MeetingRoomResponse> addRoom(@RequestBody MeetingRoomRequest request) {
        MeetingRoomResponse response = meetingRoomService.addRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public MeetingRoomResponse updateRoom(@PathVariable Long id, @RequestBody MeetingRoomRequest request) {
        return meetingRoomService.updateRoom(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteRoom(@PathVariable Long id) {
        meetingRoomService.deleteRoom(id);
    }

    @GetMapping
    public List<MeetingRoomResponse> getAllRooms() {
        return meetingRoomService.getAllRooms();
    }

}