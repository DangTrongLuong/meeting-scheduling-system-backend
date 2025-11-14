package com.meeting.schedule_a_meeting.service.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.MeetingRoomRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.MeetingRoomResponse;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import com.meeting.schedule_a_meeting.exception.AppException;
import com.meeting.schedule_a_meeting.mapper.admin.MeetingRoomMapper;
import com.meeting.schedule_a_meeting.repositories.MeetingRoomRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MeetingRoomService {

    private final MeetingRoomRepository repository;
    private final MeetingRoomMapper mapper;

    @Transactional
    public MeetingRoomResponse create(MeetingRoomRequest request) {
        if (repository.existsByName(request.getName())) {
            throw new AppException(ErrorStatus.ROOM_NAME_EXISTS);
        }

        MeetingRoom entity = mapper.toEntity(request);
        MeetingRoom saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    public MeetingRoomResponse getById(String id) {
        MeetingRoom room = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));
        return mapper.toResponse(room);
    }

    public List<MeetingRoomResponse> getAll(String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return repository.findAll(sort).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public MeetingRoomResponse update(String id, MeetingRoomRequest request) {
        MeetingRoom room = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorStatus.ROOM_NOT_FOUND));

        if (!room.getName().equals(request.getName()) && repository.existsByName(request.getName())) {
            throw new AppException(ErrorStatus.ROOM_NAME_EXISTS);
        }

        mapper.updateEntity(room, request);
        MeetingRoom updated = repository.save(room);
        return mapper.toResponse(updated);
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new AppException(ErrorStatus.ROOM_NOT_FOUND);
        }
        repository.deleteById(id);
    }

    public List<MeetingRoomResponse> searchByName(String name) {
        return repository.findAll().stream()
                .filter(r -> r.getName().toLowerCase().contains(name.toLowerCase()))
                .map(mapper::toResponse)
                .toList();
    }
}
