package com.meeting.schedule_a_meeting.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import com.meeting.schedule_a_meeting.enums.AuthProvider;
import com.meeting.schedule_a_meeting.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "users")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Users {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "user_id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "google_id", unique = true)
    String googleId;

    @NotBlank(message = "Name is required")
    @Size(min = 5, max = 20, message = "Name must be between 5 and 20 characters")
    @Column(name = "name", nullable = false, length = 100)
    String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    String email;

    @Column(name = "password", length = 255)
    String password;

    @Column(name = "age")
    int age;

    @Column(name = "address", length = 255)
    String address;

    @Column(name = "avatar_url", length = 500)
    String avatar_url;

    @Column(name = "access_token", length = 1000)
    String accessToken;

    @Column(name = "refresh_token", length = 1000)
    String refreshToken;

    @Column(name = "background_url", length = 500)
    String background_url;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", length = 20)
    AuthProvider authProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    Role role = Role.USER;

    @Column(name = "expires_in")
    Integer expiresIn;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDate createdAt;

    @Column(name = "is_active", nullable = false)
    boolean active = false;

    @Column(name = "verification_token", length = 36, unique = true)
    String verificationToken;

    @Column(name = "reset_code")
    String resetCode;

    @Column(name = "reset_code_expiry")
    LocalDateTime resetCodeExpiry;

    @Column(name = "reset_attempts")
    Integer resetAttempts = 0;
}
