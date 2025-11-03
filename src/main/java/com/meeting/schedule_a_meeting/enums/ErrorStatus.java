package com.meeting.schedule_a_meeting.enums;

public enum ErrorStatus {
    USER_NOTFOUND(400, "User Not Found!"),
    USER_EXISTED(400, "User does exist!");

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
