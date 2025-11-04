package com.meeting.schedule_a_meeting.enums;

public enum ErrorStatus {
    USER_NOTFOUND(400, "User Not Found!"),
    USER_NOT_EXISTED(400, "User Not Existed!"),
    INVALID_CREDENTIALS(400, "Invalid Creadentials!"),

    USER_EXISTED(400, "User does exist!"),
    INVALID_TOKEN(400, "Invalid Token!");

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
