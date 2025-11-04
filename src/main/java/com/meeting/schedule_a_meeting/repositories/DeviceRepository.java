package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    boolean existsByName(String name);
}