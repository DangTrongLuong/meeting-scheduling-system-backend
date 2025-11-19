package com.meeting.schedule_a_meeting.dto.response.users;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    boolean authenticated;
    private UUID id;
    private String accessToken;
    private long expiresIn;
    private String name;
    private String email;
    private String avatarUrl;
    private String role;
    private String backgroundUrl;
    private LocalDate createdAt;
    private String authProvider;
}
