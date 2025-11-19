package com.meeting.schedule_a_meeting.dto.response.admin;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminResponse {
    boolean authenticated;
    private UUID id;
    private String accessToken;
    private long expiresIn;
    private String name;
    private String email;
    private String avatarUrl;
    private String role;
    private int age;
    private String address;
    private LocalDate createdAt;
    private String backgroundUrl;
    private String authProvider;
}
