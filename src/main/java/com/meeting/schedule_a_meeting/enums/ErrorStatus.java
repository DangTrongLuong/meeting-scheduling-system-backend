package com.meeting.schedule_a_meeting.enums;

public enum ErrorStatus {
    // User-related errors
    USER_NOTFOUND(404, "User Not Found!"),
    USER_NOT_EXISTED(404, "User Not Existed!"),
    ADMIN_NOT_EXISTED(404, "Admin Not Existed!"),
    USER_EXISTED(409, "User does exist!"),
    USER_NOT_ACTIVATED(403, "Account not activated. Please check your email."),
    RESET_PASSWORD_NOT_ALLOWED_FOR_GOOGLE_USER(403, "Reset password is not allowed for Google user"),

    // Auth-related errors
    INVALID_CREDENTIALS(401, "Invalid email or password"),
    INVALID_TOKEN(401, "Invalid Token!"),
    UNAUTHORIZED(403, "Unauthorized"),

    // Device-related errors
    DEVICE_ALREADY_EXISTS(400, "Device already exists"),
    DEVICE_NOT_FOUND(404, "Device not found");

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