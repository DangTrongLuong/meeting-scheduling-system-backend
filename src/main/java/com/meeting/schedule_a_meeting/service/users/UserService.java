package com.meeting.schedule_a_meeting.service.users;

import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Users register(Users user) {
        return userRepository.save(user);
    }

    public Optional<Users> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}