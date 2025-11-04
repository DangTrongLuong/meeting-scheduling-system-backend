package com.meeting.schedule_a_meeting.service.users;

import java.util.Collections;

import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;



@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        String password = user.getPassword() != null ? user.getPassword() : ""; // Xử lý password null cho user Google

        String role = user.getRole() != null ? "ROLE_" + user.getRole() : "ROLE_USER"; // Sử dụng role từ DB

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
}
