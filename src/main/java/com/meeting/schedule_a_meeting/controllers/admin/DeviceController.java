package com.meeting.schedule_a_meeting.controllers.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.DeviceCreateRequest;
import com.meeting.schedule_a_meeting.dto.request.admin.DeviceUpdateRequest;
import com.meeting.schedule_a_meeting.dto.response.PagedResponse;
import com.meeting.schedule_a_meeting.dto.response.admin.DeviceResponse;
import com.meeting.schedule_a_meeting.dto.response.admin.MeetingRoomResponse;
import com.meeting.schedule_a_meeting.enums.DeviceStatus;
import com.meeting.schedule_a_meeting.service.admin.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

import com.meeting.schedule_a_meeting.dto.response.PagedResponse;


@RestController
@RequestMapping("/api/admin/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DeviceResponse> create(@Valid @ModelAttribute DeviceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getAll(
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(service.getAll(sortBy, direction));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DeviceResponse>> getByStatus(@PathVariable DeviceStatus status) {
        return ResponseEntity.ok(service.getByStatus(status));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DeviceResponse> update(
            @PathVariable String id,
            @Valid @ModelAttribute DeviceUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(params = {"page", "size"})
    public ResponseEntity<PagedResponse<DeviceResponse>> getPaged(
            @RequestParam int page,                          // UI: 1-based
            @RequestParam int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search
    ) {
        Page<DeviceResponse> resPage =
                service.getPaged(page, size, sortBy, direction, search);

        PagedResponse<DeviceResponse> body = new PagedResponse<>(
                resPage.getContent(),
                resPage.getNumber() + 1,      // trả về 1-based cho FE
                resPage.getSize(),
                resPage.getTotalElements(),
                resPage.getTotalPages()
        );
        return ResponseEntity.ok(body);
    }
}