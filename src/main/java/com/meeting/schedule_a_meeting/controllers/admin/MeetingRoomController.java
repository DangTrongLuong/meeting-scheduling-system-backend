
package com.meeting.schedule_a_meeting.controllers.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.MeetingRoomRequest;
import com.meeting.schedule_a_meeting.dto.response.PagedResponse;                // [ADD]
import com.meeting.schedule_a_meeting.dto.response.admin.MeetingRoomResponse;
import com.meeting.schedule_a_meeting.service.admin.MeetingRoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;                                       // [ADD]
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

    // ------------------------------------------------------------
    // [ADD] Endpoint phân trang (cùng path, dùng params page & size)
    //      Ví dụ: GET /api/admin/rooms?page=1&size=10&sortBy=name&direction=asc&search=abc
    // ------------------------------------------------------------
    @GetMapping(params = {"page", "size"})
    public ResponseEntity<PagedResponse<MeetingRoomResponse>> getPaged(
            @RequestParam int page,                          // UI: 1-based
            @RequestParam int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search
    ) {
        Page<MeetingRoomResponse> resPage =
                service.getPaged(page, size, sortBy, direction, search);

        PagedResponse<MeetingRoomResponse> body = new PagedResponse<>(
                resPage.getContent(),
                resPage.getNumber() + 1,      // trả về 1-based cho FE
                resPage.getSize(),
                resPage.getTotalElements(),
                resPage.getTotalPages()
        );
        return ResponseEntity.ok(body);
    }
}