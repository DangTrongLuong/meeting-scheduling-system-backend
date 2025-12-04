package com.meeting.schedule_a_meeting.service.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.DeviceCreateRequest;
import com.meeting.schedule_a_meeting.dto.request.admin.DeviceUpdateRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.DeviceResponse;
import com.meeting.schedule_a_meeting.dto.response.admin.MeetingRoomResponse;
import com.meeting.schedule_a_meeting.entities.Device;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.enums.DeviceStatus;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.mapper.admin.DeviceMapper;
import com.meeting.schedule_a_meeting.repositories.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import org.springframework.data.domain.Sort;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository repository;
    private final DeviceMapper mapper;
    private static final String UPLOAD_DIR = "uploads/devices/";

    @jakarta.annotation.PostConstruct
    public void init() {
        new File(UPLOAD_DIR).mkdirs();
    }

    // CREATE
    @Transactional
    public DeviceResponse create(DeviceCreateRequest request) {
        validateImage(request.getImage());
        String imagePath = saveImage(request.getImage());

        Optional<Device> existing = repository.findByNameAndStatus(request.getName(), request.getStatus());
        if (existing.isPresent()) {
            Device device = existing.get();
            device.setTotalQuantity(device.getTotalQuantity() + request.getQuantity());
            device.setAvailableQuantity(device.getAvailableQuantity() + request.getQuantity());
            device.setImagePath(imagePath);
            return mapper.toResponse(repository.save(device));
        }

        Device device = mapper.toEntity(request);
        device.setImagePath(imagePath);
        return mapper.toResponse(repository.save(device));
    }

    // READ - Get by ID
    public DeviceResponse getById(String id) {
        Device device = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_NOT_FOUND));
        return mapper.toResponse(device);
    }

    // READ - Get all with sorting
    public List<DeviceResponse> getAll(String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return repository.findAll(sort).stream()
                .map(mapper::toResponse)
                .toList();
    }

    // READ - Get by status
    public List<DeviceResponse> getByStatus(DeviceStatus status) {
        return repository.findAll().stream()
                .filter(d -> d.getStatus() == status)
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public DeviceResponse update(String id, DeviceUpdateRequest request) {
        Device device = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.DEVICE_NOT_FOUND));

        String newImagePath = device.getImagePath();
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            validateImage(request.getImage());
            if (newImagePath != null) {
                deleteOldImage(newImagePath);
            }
            newImagePath = saveImage(request.getImage());
        }

        boolean nameChanged = !device.getName().equals(request.getName());
        boolean statusChanged = device.getStatus() != request.getStatus();

        if ((nameChanged || statusChanged)) {
            Optional<Device> duplicate = repository.findByNameAndStatus(request.getName(), request.getStatus());
            if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                throw new AppException(ErrorStatus.DEVICE_NAME_EXISTS);
            }
        }

        int oldAvailable = device.getAvailableQuantity();
        int oldTotal = device.getTotalQuantity();
        int newQuantity = request.getQuantity();

        device.setName(request.getName());
        device.setStatus(request.getStatus());
        device.setTotalQuantity(newQuantity);
        device.setAvailableQuantity(newQuantity - (oldTotal - oldAvailable)); // giữ nguyên số lượng đã dùng
        device.setImagePath(newImagePath);

        return mapper.toResponse(repository.save(device));
    }

    // DELETE
    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new AppException(ErrorStatus.DEVICE_NOT_FOUND);
        }
        repository.deleteById(id);
    }

    // PRIVATE: Image validation & save
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorStatus.INVALID_IMAGE_FORMAT);
        }
        if (!file.getContentType().startsWith("image/")) {
            throw new AppException(ErrorStatus.INVALID_IMAGE_FORMAT);
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new AppException(ErrorStatus.IMAGE_TOO_LARGE);
        }
    }

    private String saveImage(MultipartFile file) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/devices/" + fileName;
        } catch (IOException e) {
            throw new AppException(ErrorStatus.IMAGE_UPLOAD_FAILED);
        }
    }

    private void deleteOldImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty())
            return;

        try {

            Path filePath = Paths.get("uploads/devices", imagePath.substring("/uploads/devices/".length()));
            Files.deleteIfExists(filePath);
        } catch (Exception e) {

            System.err.println("Failed to delete old image: " + imagePath + " | " + e.getMessage());
        }
    }

    public Page<DeviceResponse> getPaged(int page, int size, String sortBy, String direction, String search) {
        // Spring Pageable dùng pageIndex 0-based
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.max(size, 1);

        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);

        Page<Device> entityPage;
        if (search != null && !search.trim().isEmpty()) {
            entityPage = repository.findByNameContainingIgnoreCase(search.trim(), pageable);
        } else {
            entityPage = repository.findAll(pageable);
        }

        // map entity -> dto
        return entityPage.map(mapper::toResponse);
    }
}