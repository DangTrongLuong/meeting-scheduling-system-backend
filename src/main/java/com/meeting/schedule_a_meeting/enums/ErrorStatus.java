package com.meeting.schedule_a_meeting.enums;

import org.springframework.http.HttpStatus;

public enum ErrorStatus {
    // User-related errors
    USER_NOT_FOUND(404, "User Not Found!"),
    USER_NOT_EXISTED(404, "User Not Existed!"),

    ADMIN_NOT_EXISTED(404, "Admin Not Existed!"),
    USER_EXISTED(409, "User already exists!"),
    EMAIL_EXISTED(409, "Email already exists!"),
    EMAIL_NOT_EXISTED(409, "Email not exists!"),
    EMAIL_USED_BY_LOCAL(409, "Email has been registered with a local account. Please log in with your password."),
    USER_NOT_ACTIVATED(403, "Account not activated. Please check your email."),
    RESET_PASSWORD_NOT_ALLOWED_FOR_GOOGLE_USER(403, "Reset password is not allowed for Google user"),

    // Auth-related errors
    INVALID_CREDENTIALS(401, "Invalid email or password"),
    INVALID_TOKEN(401, "Invalid Token!"),
    UNAUTHORIZED(403, "Unauthorized"),
    FORBIDDEN(403, "You don't have permission to perform this action!"),

    // Device-related errors
    DEVICE_ALREADY_EXISTS(400, "Device already exists"),
    ROOM_NOT_FOUND(404, "Room not found"),
    DEVICE_NOT_IN_ROOM(400, "Device is not available in this room!"),
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

    // Participant errors
    PARTICIPANT_NOT_FOUND(404, "Participant not found!"),
    PARTICIPANT_ALREADY_INVITED(409, "User is already invited to this meeting!"),
    PARTICIPANT_CANNOT_REMOVE_CREATOR(400, "Cannot remove meeting creator from participants!"),

    // Validation errors
    INVALID_INPUT(400, "Invalid input data!"),
    INVALID_TIME_FORMAT(400, "Invalid time format!"),

    // Meeting errors
    MEETING_NOT_FOUND(404, "Meeting not found!"),
    MEETING_ROOM_NOT_AVAILABLE(409, "Meeting room is not available at the selected time!"),
    MEETING_TIME_CONFLICT(409, "Meeting time conflicts with another meeting in the same room!"),
    MEETING_INVALID_TIME_RANGE(400, "Meeting time must be within 7:00-12:00 or 13:00-23:00!"),
    MEETING_END_BEFORE_START(400, "Meeting end time must be after start time!"),
    MEETING_ALREADY_CANCELLED(400, "Meeting is already cancelled!"),
    MEETING_CANNOT_EDIT_PAST(400, "Cannot edit past meetings!"),
    MEETING_CREATOR_REQUIRED(403, "Only meeting creator can perform this action!"),

    // Two-Factor Authentication errors
    TWO_FACTOR_REQUIRED(428, "2FA is required for this account"),
    INVALID_2FA_CODE(401, "Invalid 2FA code"),
    TWO_FACTOR_NOT_ENABLED(400, "2FA is not enabled for this account"),

    INTERNAL_SERVER_ERROR(500,"Internal Server Errol"),

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