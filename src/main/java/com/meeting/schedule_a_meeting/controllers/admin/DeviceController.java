package com.meeting.schedule_a_meeting.controllers.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.DeviceRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.DeviceResponse;
import com.meeting.schedule_a_meeting.service.admin.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(@Valid @RequestBody DeviceRequest request) {
        DeviceResponse response = deviceService.createDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created
    }



}
