package com.meeting.schedule_a_meeting.enums;

import org.springframework.http.HttpStatus;

public enum ErrorStatus {
    // User-related errors
    USER_NOTFOUND(404, "User Not Found!"),
    USER_NOT_EXISTED(404, "User Not Existed!"),
    ADMIN_NOT_EXISTED(404, "Admin Not Existed!"),
    USER_EXISTED(409, "User already exists!"),
    EMAIL_EXISTED(409, "Email already exists!"),
    EMAIL_USED_BY_LOCAL(409, "Email has been registered with a local account. Please log in with your password."),
    USER_NOT_ACTIVATED(403, "Account not activated. Please check your email."),
    RESET_PASSWORD_NOT_ALLOWED_FOR_GOOGLE_USER(403, "Reset password is not allowed for Google user"),

    // Auth-related errors
    INVALID_CREDENTIALS(401, "Invalid email or password"),
    INVALID_TOKEN(401, "Invalid Token!"),
    UNAUTHORIZED(403, "Unauthorized"),

    // Device-related errors
    DEVICE_ALREADY_EXISTS(400, "Device already exists"),
    ROOM_NOT_FOUND(404, "Room not found"),
    ROOM_NAME_EXISTS(409, "Room name already exists"),
    DEVICE_NOT_FOUND(404, "Device not found"),
    DEVICE_NAME_EXISTS(409, "Device with this name and status already exists"),
    DEVICE_NOT_ACTIVE(400, "Only ACTIVE devices can be assigned"),
    INSUFFICIENT_QUANTITY(400, "Not enough devices available"),
    ROOM_DEVICE_NOT_FOUND(404, "Assignment not found"),
    DEVICE_NOT_AVAILABLE(400, "Device is not available"),
    INVALID_IMAGE_FORMAT(400, "Invalid image format"),
    IMAGE_TOO_LARGE(400, "Image size must be < 5MB"),
    DEVICE_ALREADY_IN_USE_IN_ROOM(400, "Device is already in use in this room"),
    IMAGE_UPLOAD_FAILED(500, "Failed to upload image");

    private final int status;
    private final String message;

    ErrorStatus(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}