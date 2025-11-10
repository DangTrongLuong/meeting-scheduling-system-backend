package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    boolean existsByName(String name);
}
