package com.meeting.schedule_a_meeting.exception;

import com.meeting.schedule_a_meeting.enums.ErrorStatus;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus
public class AppException extends RuntimeException {
    private final ErrorStatus errorStatus;
    private final String customMessage;

    public AppException(ErrorStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
        this.customMessage = errorStatus.getMessage();
    }

    public AppException(ErrorStatus errorStatus, String customMessage) {
        super(customMessage);
        this.errorStatus = errorStatus;
        this.customMessage = customMessage;
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(errorStatus.getStatus());
    }
}