package com.meeting.schedule_a_meeting.controllers.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.DeviceCreateRequest;
import com.meeting.schedule_a_meeting.dto.request.admin.DeviceUpdateRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.DeviceResponse;
import com.meeting.schedule_a_meeting.enums.DeviceStatus;
import com.meeting.schedule_a_meeting.service.admin.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

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
}