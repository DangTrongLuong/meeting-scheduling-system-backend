package com.meeting.schedule_a_meeting.service.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.AssignDeviceRequest;
import com.meeting.schedule_a_meeting.dto.request.admin.UpdateAssignmentRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.RoomDeviceResponse;
import com.meeting.schedule_a_meeting.entities.Device;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.entities.RoomDevice;
import com.meeting.schedule_a_meeting.enums.DeviceStatus;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.enums.RoomDeviceStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.mapper.admin.RoomDeviceMapper;
import com.meeting.schedule_a_meeting.repositories.DeviceRepository;
import com.meeting.schedule_a_meeting.repositories.MeetingRoomRepository;
import com.meeting.schedule_a_meeting.repositories.RoomDeviceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomDeviceService {

        private final RoomDeviceRepository rdRepo;
        private final MeetingRoomRepository roomRepo;
        private final DeviceRepository deviceRepo;
        private final RoomDeviceMapper mapper;

        @Transactional
        public RoomDeviceResponse assign(AssignDeviceRequest request) {
                MeetingRoom room = roomRepo.findById(request.getRoomId())
                                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));

                Device device = deviceRepo.findById(request.getDeviceId())
                                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_NOT_FOUND));

                if (device.getStatus() != DeviceStatus.ACTIVE) {
                        throw new AppException(ErrorStatus.DEVICE_NOT_ACTIVE);
                }

                if (device.getAvailableQuantity() < request.getQuantity()) {
                        throw new AppException(ErrorStatus.INSUFFICIENT_QUANTITY);
                }

                List<RoomDevice> existingList = rdRepo.findByMeetingRoomIdAndDeviceId(
                                request.getRoomId(), request.getDeviceId());

                boolean hasInUse = existingList.stream()
                                .anyMatch(rd -> rd.getStatus() == RoomDeviceStatus.IN_USE);

                if (hasInUse) {
                        throw new AppException(ErrorStatus.DEVICE_ALREADY_IN_USE_IN_ROOM);
                }

                existingList.stream()
                                .filter(rd -> rd.getStatus() == RoomDeviceStatus.RETURNED)
                                .forEach(rd -> {
                                        device.setAvailableQuantity(device.getAvailableQuantity() + rd.getQuantity());
                                });

                device.setAvailableQuantity(device.getAvailableQuantity() - request.getQuantity());
                deviceRepo.save(device);

                RoomDevice newAssignment = RoomDevice.builder()
                                .meetingRoom(room)
                                .device(device)
                                .quantity(request.getQuantity())
                                .status(RoomDeviceStatus.IN_USE)
                                .build();

                return mapper.toResponse(rdRepo.save(newAssignment));
        }

        public List<RoomDeviceResponse> getAll() {
                return rdRepo.findAll().stream()
                                .map(mapper::toResponse)
                                .toList();
        }

        public List<RoomDeviceResponse> getByRoom(String roomId) {
                if (!roomRepo.existsById(roomId)) {
                        throw new AppException(ErrorStatus.ROOM_NOT_FOUND);
                }
                return rdRepo.findByMeetingRoomId(roomId).stream()
                                .map(mapper::toResponse)
                                .toList();
        }

        // READ - Get by ID
        public RoomDeviceResponse getById(String id) {
                RoomDevice rd = rdRepo.findById(id)
                                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_DEVICE_NOT_FOUND));
                return mapper.toResponse(rd);
        }

        // REMOVE (DELETE)
        @Transactional
        public void remove(String id) {
                RoomDevice rd = rdRepo.findById(id)
                                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_DEVICE_NOT_FOUND));

                Device device = rd.getDevice();
                device.setAvailableQuantity(device.getAvailableQuantity() + rd.getQuantity());
                deviceRepo.save(device);

                rdRepo.delete(rd);
        }

        // UPDATE STATUS (e.g., mark as DAMAGED)
        @Transactional
        public RoomDeviceResponse updateStatus(String id, RoomDeviceStatus status) {
                RoomDevice rd = rdRepo.findById(id)
                                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_DEVICE_NOT_FOUND));

                if (rd.getStatus() == RoomDeviceStatus.IN_USE && status == RoomDeviceStatus.RETURNED) {
                        Device device = rd.getDevice();
                        device.setAvailableQuantity(device.getAvailableQuantity() + rd.getQuantity());
                        deviceRepo.save(device);
                }

                rd.setStatus(status);
                return mapper.toResponse(rdRepo.save(rd));
        }

        @Transactional
        public RoomDeviceResponse updateAssignment(String id, UpdateAssignmentRequest request) {
                RoomDevice roomDevice = rdRepo.findById(id)
                                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_DEVICE_NOT_FOUND));

                Device oldDevice = roomDevice.getDevice();
                int oldQuantity = roomDevice.getQuantity();

                // Update room if provided
                if (request.getRoomId() != null) {
                        MeetingRoom newRoom = roomRepo.findById(request.getRoomId())
                                        .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));
                        roomDevice.setMeetingRoom(newRoom);
                }

                // Update device if provided
                if (request.getDeviceId() != null) {
                        Device newDevice = deviceRepo.findById(request.getDeviceId())
                                        .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_NOT_FOUND));

                        if (newDevice.getStatus() != DeviceStatus.ACTIVE) {
                                throw new AppException(ErrorStatus.DEVICE_NOT_AVAILABLE);
                        }

                        // Return old quantity to old device
                        oldDevice.setAvailableQuantity(oldDevice.getAvailableQuantity() + oldQuantity);
                        deviceRepo.save(oldDevice);

                        roomDevice.setDevice(newDevice);
                }

                // Update quantity if provided
                if (request.getQuantity() != null) {
                        int newQuantity = request.getQuantity();
                        int delta = newQuantity - oldQuantity;

                        if (delta > 0 && roomDevice.getDevice().getAvailableQuantity() < delta) {
                                throw new AppException(ErrorStatus.INSUFFICIENT_QUANTITY);
                        }

                        roomDevice.setQuantity(newQuantity);
                        Device currentDevice = roomDevice.getDevice();
                        currentDevice.setAvailableQuantity(currentDevice.getAvailableQuantity() - delta);
                        deviceRepo.save(currentDevice);
                }

                // Update status if provided
                if (request.getStatus() != null) {
                        RoomDeviceStatus newStatus = request.getStatus();
                        if (roomDevice.getStatus() == RoomDeviceStatus.IN_USE
                                        && newStatus == RoomDeviceStatus.RETURNED) {
                                Device device = roomDevice.getDevice();
                                device.setAvailableQuantity(device.getAvailableQuantity() + roomDevice.getQuantity());
                                deviceRepo.save(device);
                        }
                        roomDevice.setStatus(newStatus);
                }

                return mapper.toResponse(rdRepo.save(roomDevice));
        }
}