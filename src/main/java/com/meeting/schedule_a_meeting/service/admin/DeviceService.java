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

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    // ✅ Tạo thiết bị
    public DeviceResponse createDevice(DeviceRequest request) {
        if (deviceRepository.existsByName(request.getName())) {
            throw new AppException(ErrorStatus.DEVICE_ALREADY_EXISTS);
        }

        Device device = deviceMapper.toEntity(request);

        // Nếu active chưa set thì mặc định true
        if (!device.isActive()) {
            device.setActive(true);
        }

        // Nếu quantity < 0 thì set về 0
        if (device.getQuantity() < 0) {
            device.setQuantity(0);
        }

        return deviceMapper.toResponse(deviceRepository.save(device));
    }

    // ✅ Cập nhật thiết bị
    public DeviceResponse updateDevice(Long id, DeviceRequest request) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_NOT_FOUND));

        if (!device.getName().equals(request.getName()) && deviceRepository.existsByName(request.getName())) {
            throw new AppException(ErrorStatus.DEVICE_ALREADY_EXISTS);
        }

        device.setName(request.getName());
        device.setActive(request.isActive());
        device.setQuantity(request.getQuantity());

        return deviceMapper.toResponse(deviceRepository.save(device));
    }


}
