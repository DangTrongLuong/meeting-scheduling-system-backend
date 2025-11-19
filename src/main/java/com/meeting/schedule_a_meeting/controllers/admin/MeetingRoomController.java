package com.meeting.schedule_a_meeting.controllers.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.MeetingRoomRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.MeetingRoomResponse;
import com.meeting.schedule_a_meeting.service.admin.MeetingRoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
@Validated
public class MeetingRoomController {
    private final MeetingRoomService service;

    @PostMapping
    public ResponseEntity<MeetingRoomResponse> create(@Valid @RequestBody MeetingRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeetingRoomResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<MeetingRoomResponse>> getAll(
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(service.getAll(sortBy, direction));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MeetingRoomResponse>> search(@RequestParam String name) {
        return ResponseEntity.ok(service.searchByName(name));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MeetingRoomResponse> update(
            @PathVariable String id,
            @Valid @RequestBody MeetingRoomRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}