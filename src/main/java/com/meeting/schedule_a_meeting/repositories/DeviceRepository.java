package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.Device;
import com.meeting.schedule_a_meeting.enums.DeviceStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {

    boolean existsByNameAndIdNot(String name, String id);

    Optional<Device> findByName(String name);

    boolean existsByName(String name);

    Optional<Device> findByNameAndStatus(String name, DeviceStatus status);

    boolean existsByNameAndStatus(String name, DeviceStatus status);
}
