package com.meeting.schedule_a_meeting.service.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.DeviceRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.DeviceResponse;
import com.meeting.schedule_a_meeting.entities.Device;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.mapper.admin.DeviceMapper;
import com.meeting.schedule_a_meeting.repositories.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    public DeviceResponse createDevice(DeviceRequest request) {
        if (deviceRepository.existsByName(request.getName())) {
            throw new AppException(ErrorStatus.DEVICE_ALREADY_EXISTS);
        }
        Device device = deviceMapper.toEntity(request);
        return deviceMapper.toResponse(deviceRepository.save(device));
    }

    public DeviceResponse updateDevice(UUID id, DeviceRequest request) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_NOT_FOUND));
        device.setName(request.getName());
        device.setDescription(request.getDescription());
        device.setActive(request.isActive());
        return deviceMapper.toResponse(deviceRepository.save(device));
    }

    public void deleteDevice(UUID id) {
        if (!deviceRepository.existsById(id)) {
            throw new AppException(ErrorStatus.DEVICE_NOT_FOUND);
        }
        deviceRepository.deleteById(id);
    }

    public List<DeviceResponse> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(deviceMapper::toResponse)
                .toList();
    }
}