package com.meeting.schedule_a_meeting.repositories;

import com.meeting.schedule_a_meeting.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, String> {
    boolean existsByEmail(String email);
}