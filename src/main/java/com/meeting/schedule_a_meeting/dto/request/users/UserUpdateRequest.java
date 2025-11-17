package com.meeting.schedule_a_meeting.dto.request.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class UserUpdateRequest {

    @Size(min = 5, max = 20, message = "Name must be between 5 and 20 characters")
    String name;

    @Size(min = 0, max = 100, message = "Age must be between 0 and 100 characters")
    int age;

    String address;
}
