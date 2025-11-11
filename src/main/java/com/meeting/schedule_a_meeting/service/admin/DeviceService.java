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
import java.util.Optional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    //  Tạo mới thiết bị (Add)
    public DeviceResponse createDevice(DeviceRequest request) {
        // Kiểm tra nếu đã có thiết bị cùng tên và cùng trạng thái
        Optional<Device> existingDeviceOpt = deviceRepository.findByNameAndActive(request.getName(), request.isActive());

        if (existingDeviceOpt.isPresent()) {
            // Nếu tồn tại cùng tên và cùng trạng thái → cộng thêm số lượng
            Device existingDevice = existingDeviceOpt.get();
            int newQuantity = existingDevice.getQuantity() + request.getQuantity();
            existingDevice.setQuantity(newQuantity);
            return deviceMapper.toResponse(deviceRepository.save(existingDevice));
        }

        // Nếu chưa tồn tại hoặc khác trạng thái → tạo mới
        Device device = deviceMapper.toEntity(request);



        // Nếu quantity < 0 thì set về 0
        if (device.getQuantity() < 0) {
            device.setQuantity(0);
        }

        return deviceMapper.toResponse(deviceRepository.save(device));
    }


    //  Xóa thiết bị
    public void deleteDevice(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new AppException(ErrorStatus.DEVICE_NOT_FOUND);
        }
        deviceRepository.deleteById(id);
    }

    //  Cập nhật thiết bị (Edit)
    public DeviceResponse updateDevice(Long id, DeviceRequest request) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_NOT_FOUND));

        boolean isActiveChanged = device.isActive() != request.isActive();

        if (isActiveChanged) {
            // Nếu đổi trạng thái → kiểm tra bản ghi cùng tên và trạng thái mới
            Optional<Device> existing = deviceRepository.findByNameAndActive(request.getName(), request.isActive());
            if (existing.isPresent()) {
                // Nếu có → cộng quantity vào bản ghi đó
                Device targetDevice = existing.get();
                targetDevice.setQuantity(targetDevice.getQuantity() + request.getQuantity());

                // ✅ Xóa bản ghi cũ
                deviceRepository.delete(device);

                return deviceMapper.toResponse(deviceRepository.save(targetDevice));
            }

            // Nếu chưa có → tạo bản ghi mới
            Device newDevice = new Device();
            newDevice.setName(request.getName());
            newDevice.setQuantity(request.getQuantity());
            newDevice.setActive(request.isActive());

            //  Xóa bản ghi cũ
            deviceRepository.delete(device);

            return deviceMapper.toResponse(deviceRepository.save(newDevice));
        }

        // Nếu không đổi trạng thái → update bình thường
        device.setName(request.getName());
        device.setQuantity(request.getQuantity());
        return deviceMapper.toResponse(deviceRepository.save(device));
    }



    //  Lấy tất cả thiết bị
    public List<DeviceResponse> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(deviceMapper::toResponse)
                .toList();
    }

    //  Lấy chi tiết thiết bị theo ID
    public DeviceResponse getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_NOT_FOUND));
        return deviceMapper.toResponse(device);
    }
}