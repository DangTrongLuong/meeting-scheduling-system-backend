package com.meeting.schedule_a_meeting.enums;

public enum ErrorStatus {
    USER_NOTFOUND(404, "User Not Found!"),
    USER_NOT_EXISTED(404, "User Not Existed!"),
    INVALID_CREDENTIALS(401, "Invalid email or password"),

    DEVICE_ALREADY_EXISTS(400, "Device already exists"),
    DEVICE_NOT_FOUND(404, "Device not found"),

    USER_EXISTED(409, "User does exist!"),
    USER_NOT_ACTIVATED(403, "Account not activated. Please check your email."),
    INVALID_TOKEN(401, "Invalid Token!"),

    RESET_PASSWORD_NOT_ALLOWED_FOR_GOOGLE_USER(403, "Tài khoản của bạn đăng nhập bằng Google. Vui lòng đổi mật khẩu trực tiếp trong tài khoản Google.");

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