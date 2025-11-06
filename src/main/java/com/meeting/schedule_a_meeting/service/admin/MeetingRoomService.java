package com.meeting.schedule_a_meeting.service.admin;

import com.meeting.schedule_a_meeting.dto.request.admin.MeetingRoomRequest;
import com.meeting.schedule_a_meeting.dto.response.admin.MeetingRoomResponse;
import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import com.meeting.schedule_a_meeting.repositories.MeetingRoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MeetingRoomService {
    private final MeetingRoomRepository meetingRoomRepository;

    public MeetingRoomService(MeetingRoomRepository meetingRoomRepository) {
        this.meetingRoomRepository = meetingRoomRepository;
    }

    //them_phong
    public MeetingRoomResponse addRoom(MeetingRoomRequest request) {
        MeetingRoom room = new MeetingRoom(null, request.getName(), request.getLocation(), request.getCapacity());
        MeetingRoom saved = meetingRoomRepository.save(room);
        return new MeetingRoomResponse(saved.getId(), saved.getName(), saved.getLocation(), saved.getCapacity());
    }

    public MeetingRoomResponse updateRoom(Long id, MeetingRoomRequest request) {
        MeetingRoom room = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        room.setName(request.getName());
        room.setLocation(request.getLocation());
        room.setCapacity(request.getCapacity());
        MeetingRoom updated = meetingRoomRepository.save(room);
        return new MeetingRoomResponse(updated.getId(), updated.getName(), updated.getLocation(), updated.getCapacity());
    }



}
