package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class AppClientException extends AppException {

    private final int httpStatusCode;

    public AppClientException(int httpStatusCode, AppErrorCodeMessageEnum error, String reason, Object... args) {
        super(error, reason, args);
        this.httpStatusCode = httpStatusCode;
    }
    public AppClientException(Throwable cause, int httpStatusCode, AppErrorCodeMessageEnum error, String reason, Object... args) {
        super(cause, error, reason, args);
        this.httpStatusCode = httpStatusCode;
    }
}
