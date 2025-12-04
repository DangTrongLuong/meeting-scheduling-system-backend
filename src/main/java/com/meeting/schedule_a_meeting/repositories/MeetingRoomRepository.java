
package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.MeetingRoom;
import org.springframework.data.domain.Page;            // [ADD]
import org.springframework.data.domain.Pageable;     // [ADD]
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, String> {
    Optional<MeetingRoom> findByName(String name);

    Optional<MeetingRoom> findById(String id);

    boolean existsByName(String name);

    // [ADD] Tìm kiếm theo tên có phân trang
    Page<MeetingRoom> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // [ADD OPTIONAL] Nếu cần tìm theo location/capacity thì bổ sung tương tự:
    // Page<MeetingRoom> findByLocationContainingIgnoreCase(String location, Pageable pageable);
}
