package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final AppErrorCodeMessageEnum error;
    private final transient Object[] args;
    private final String reason;

    public AppException(AppErrorCodeMessageEnum error, String reason, Object... args) {
        super(reason);
        this.error = error;
        this.reason = reason;
        this.args = getArgsOrNull(args);
    }

    public AppException(Throwable cause, AppErrorCodeMessageEnum error, String reason, Object... args) {
        super(reason, cause);
        this.error = error;
        this.reason = reason;
        this.args = getArgsOrNull(args);
    }

    private static Object[] getArgsOrNull(Object... args) {
        return args.length > 0 ? args.clone() : null;
    }

}