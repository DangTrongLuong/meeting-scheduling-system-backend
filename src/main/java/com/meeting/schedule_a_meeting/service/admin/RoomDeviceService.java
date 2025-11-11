package com.meeting.schedule_a_meeting.service.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.AssignDeviceRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.RoomDeviceResponse;
import com.meeting.schedule_a_meeting.entities.Device;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.entities.RoomDevice;
import com.meeting.schedule_a_meeting.repositories.DeviceRepository;
import com.meeting.schedule_a_meeting.repositories.MeetingRoomRepository;
import com.meeting.schedule_a_meeting.repositories.RoomDeviceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomDeviceService {

    private final RoomDeviceRepository roomDeviceRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final DeviceRepository deviceRepository;

    public RoomDeviceService(RoomDeviceRepository roomDeviceRepository,
                             MeetingRoomRepository meetingRoomRepository,
                             DeviceRepository deviceRepository) {
        this.roomDeviceRepository = roomDeviceRepository;
        this.meetingRoomRepository = meetingRoomRepository;
        this.deviceRepository = deviceRepository;
    }

    public RoomDeviceResponse assignDeviceToRoom(AssignDeviceRequest request) {
        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Meeting room not found"));

        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (device.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Not enough devices available");
        }

        // Giảm số lượng thiết bị còn lại
        device.setQuantity(device.getQuantity() - request.getQuantity());
        deviceRepository.save(device);

        RoomDevice roomDevice = RoomDevice.builder()
                .meetingRoom(room)
                .device(device)
                .quantity(request.getQuantity())
                .status("IN_USE")
                .build();

        RoomDevice saved = roomDeviceRepository.save(roomDevice);

        return new RoomDeviceResponse(saved.getId(), room.getName(), device.getName(), saved.getQuantity(), saved.getStatus());
    }

    public List<RoomDeviceResponse> getDevicesByRoom(Long roomId) {
        return roomDeviceRepository.findByMeetingRoomId(roomId).stream()
                .map(rd -> new RoomDeviceResponse(rd.getId(), rd.getMeetingRoom().getName(),
                        rd.getDevice().getName(), rd.getQuantity(), rd.getStatus()))
                .collect(Collectors.toList());
    }

    public void removeDeviceFromRoom(Long roomDeviceId) {
        RoomDevice roomDevice = roomDeviceRepository.findById(roomDeviceId)
                .orElseThrow(() -> new RuntimeException("RoomDevice not found"));

        // Trả lại số lượng thiết bị về kho
        Device device = roomDevice.getDevice();
        device.setQuantity(device.getQuantity() + roomDevice.getQuantity());
        deviceRepository.save(device);

        roomDeviceRepository.delete(roomDevice);
    }
}