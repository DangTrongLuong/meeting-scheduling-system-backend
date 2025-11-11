package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    // Giữ nguyên các method bạn đã có
    boolean existsByNameAndIdNot(String name, Long id);
    Optional<Device> findByNameAndActive(String name, boolean active);

    // Thêm các method cần thiết
    Optional<Device> findByName(String name); // Tìm theo tên
    boolean existsByName(String name);       // Kiểm tra tồn tại theo tên
}
