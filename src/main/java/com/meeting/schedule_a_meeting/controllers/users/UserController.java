package com.meeting.schedule_a_meeting.controllers.users;

import com.meeting.schedule_a_meeting.entities.Users;
import com.meeting.schedule_a_meeting.service.users.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Users register(@RequestBody Users user) {
        return userService.register(user);
    }
}
