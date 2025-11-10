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

    // ✅ Tạo hoặc cập nhật thiết bị nếu tên đã tồn tại
    public DeviceResponse createDevice(DeviceRequest request) {
        // Tìm thiết bị theo tên
        Device existingDevice = deviceRepository.findByName(request.getName()).orElse(null);

        if (existingDevice != null) {
            // Nếu tồn tại, cộng thêm số lượng
            int newQuantity = existingDevice.getQuantity() + request.getQuantity();
            existingDevice.setQuantity(newQuantity);

            // Cập nhật trạng thái active nếu có thay đổi
            existingDevice.setActive(request.isActive());

            // Lưu lại và trả về response
            return deviceMapper.toResponse(deviceRepository.save(existingDevice));
        }

        // Nếu chưa tồn tại, tạo mới
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

    // ✅ Xóa thiết bị
    public void deleteDevice(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new AppException(ErrorStatus.DEVICE_NOT_FOUND);
        }
        deviceRepository.deleteById(id);
    }

    // ✅ Cập nhật thiết bị
    public DeviceResponse updateDevice(Long id, DeviceRequest request) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_ALREADY_EXISTS));

        // Nếu đổi tên sang tên khác đã tồn tại thì báo lỗi
        if (!device.getName().equals(request.getName()) && deviceRepository.existsByName(request.getName())) {
            throw new AppException(ErrorStatus.DEVICE_ALREADY_EXISTS);
        }

        device.setName(request.getName());
        device.setActive(request.isActive());
        device.setQuantity(request.getQuantity());

        return deviceMapper.toResponse(deviceRepository.save(device));
    }

    // ✅ Lấy tất cả thiết bị
    public List<DeviceResponse> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(deviceMapper::toResponse)
                .toList();
    }

    // ✅ Lấy chi tiết thiết bị theo ID
    public DeviceResponse getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_ALREADY_EXISTS));
        return deviceMapper.toResponse(device);
    }
}