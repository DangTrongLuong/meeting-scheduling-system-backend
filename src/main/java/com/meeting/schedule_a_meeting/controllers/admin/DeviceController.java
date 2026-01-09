package com.meeting.schedule_a_meeting.controllers.admin;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meeting.schedule_a_meeting.dto.request.admin.DeviceCreateRequest;
import com.meeting.schedule_a_meeting.dto.request.admin.DeviceUpdateRequest;
import com.meeting.schedule_a_meeting.dto.response.PagedResponse;
import com.meeting.schedule_a_meeting.dto.response.admin.DeviceResponse;
import com.meeting.schedule_a_meeting.dto.response.admin.PendingDeviceRequestDto;
import com.meeting.schedule_a_meeting.dto.response.users.meeting.ApiResponse;
import com.meeting.schedule_a_meeting.enums.DeviceStatus;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.service.admin.DeviceService;
import com.meeting.schedule_a_meeting.service.users.MeetingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService service;
    private final MeetingService meetingService;

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

    @GetMapping(params = { "page", "size" })
    public ResponseEntity<PagedResponse<DeviceResponse>> getPaged(
            @RequestParam int page, // UI: 1-based
            @RequestParam int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search) {
        Page<DeviceResponse> resPage = service.getPaged(page, size, sortBy, direction, search);

        PagedResponse<DeviceResponse> body = new PagedResponse<>(
                resPage.getContent(),
                resPage.getNumber() + 1, // trả về 1-based cho FE
                resPage.getSize(),
                resPage.getTotalElements(),
                resPage.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/rooms/{roomId}/borrow-requests")
    public ResponseEntity<List<PendingDeviceRequestDto>> getBorrowRequests(@PathVariable String roomId) {
        List<PendingDeviceRequestDto> requests = meetingService.getFrequentlyBorrowedDevices(roomId);
        return ResponseEntity.ok(requests);
    }

    // API 2: Gán cố định thiết bị vào phòng
    @PostMapping("/room-devices/assign-from-request")
    public ResponseEntity<ApiResponse<String>> assignDeviceFromRequest(@RequestBody Map<String, Object> body) {
        String roomId = (String) body.get("roomId");
        String deviceId = (String) body.get("deviceId");
        Integer quantity = (Integer) body.get("quantity");

        if (roomId == null || deviceId == null || quantity == null) {
            throw new AppException(ErrorStatus.INVALID_INPUT, "roomId, deviceId and quantity are required");
        }

        meetingService.assignDeviceToRoomPermanently(roomId, deviceId, quantity);

        return ResponseEntity.ok(ApiResponse.success("Thiết bị đã được gán cố định vào phòng thành công"));
    }
}