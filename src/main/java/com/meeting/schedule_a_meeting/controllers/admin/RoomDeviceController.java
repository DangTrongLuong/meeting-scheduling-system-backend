package com.meeting.schedule_a_meeting.controllers.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.AssignDeviceRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.RoomDeviceResponse;
import com.meeting.schedule_a_meeting.service.admin.RoomDeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/room-devices")
public class RoomDeviceController {

    private final RoomDeviceService roomDeviceService;

    public RoomDeviceController(RoomDeviceService roomDeviceService) {
        this.roomDeviceService = roomDeviceService;
    }

    @PostMapping("/assign")
    public ResponseEntity<RoomDeviceResponse> assignDevice(@RequestBody AssignDeviceRequest request) {
        RoomDeviceResponse response = roomDeviceService.assignDeviceToRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/room/{roomId}")
    public List<RoomDeviceResponse> getDevicesByRoom(@PathVariable Long roomId) {
        return roomDeviceService.getDevicesByRoom(roomId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeDevice(@PathVariable Long id) {
        roomDeviceService.removeDeviceFromRoom(id);
        return ResponseEntity.noContent().build();
    }
}