package com.meeting.schedule_a_meeting.dto.request.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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


    @NotBlank(message = "Phone number cannot be blank!")
    @Pattern(regexp = "^0\\d{9}$", message = "Phone number must start with 0 and have exactly 10 digits")
    String phone_numbers;
}
