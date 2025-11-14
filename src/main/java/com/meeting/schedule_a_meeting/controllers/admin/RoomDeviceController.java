package com.meeting.schedule_a_meeting.controllers.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.AssignDeviceRequest;
import com.meeting.schedule_a_meeting.dto.request.admin.UpdateAssignmentRequest;
import com.meeting.schedule_a_meeting.dto.request.admin.UpdateStatusRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.RoomDeviceResponse;
import com.meeting.schedule_a_meeting.enums.RoomDeviceStatus;
import com.meeting.schedule_a_meeting.service.admin.RoomDeviceService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/room-devices")
@RequiredArgsConstructor
public class RoomDeviceController {

    private final RoomDeviceService service;

    @PostMapping("/assign")
    public ResponseEntity<RoomDeviceResponse> assign(@Valid @RequestBody AssignDeviceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.assign(request));
    }

    @GetMapping
    public ResponseEntity<List<RoomDeviceResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomDeviceResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<RoomDeviceResponse>> getByRoom(@PathVariable String roomId) {
        return ResponseEntity.ok(service.getByRoom(roomId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable String id) {
        service.remove(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RoomDeviceResponse> updateStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request.status()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomDeviceResponse> updateAssignment(@PathVariable String id,
            @Valid @RequestBody UpdateAssignmentRequest request) {
        RoomDeviceResponse response = service.updateAssignment(id, request);
        return ResponseEntity.ok(response);
    }
}
