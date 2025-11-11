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

@RestController
@RequestMapping("/api/admin/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    //  Tạo mới thiết bị
    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(@Valid @RequestBody DeviceRequest request) {
        DeviceResponse response = deviceService.createDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //  Cập nhật thiết bị theo ID
    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(@PathVariable Long id,
                                                       @Valid @RequestBody DeviceRequest request) {
        DeviceResponse response = deviceService.updateDevice(id, request);
        return ResponseEntity.ok(response);
    }

    //  Xóa thiết bị theo ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    //  Lấy chi tiết thiết bị theo ID
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable Long id) {
        DeviceResponse device = deviceService.getDeviceById(id);
        return ResponseEntity.ok(device);
    }

    // Lấy tất cả thiết bị
    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getAllDevices(
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        List<DeviceResponse> devices = deviceService.getAllDevices(sortBy, direction);
        return ResponseEntity.ok(devices);
    }
}