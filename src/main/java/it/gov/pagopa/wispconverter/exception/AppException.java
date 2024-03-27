package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class AppException extends RuntimeException {

    private final transient AppErrorCodeMessageEnum codeMessage;

    private final transient Object[] args;

    public AppException(Throwable cause, AppErrorCodeMessageEnum codeMessage) {
        super(cause);
        this.codeMessage = codeMessage;
        this.args = null;
    }

    public AppException(AppErrorCodeMessageEnum codeMessage) {
        super();
        this.codeMessage = codeMessage;
        this.args = null;
    }

    public AppException(AppErrorCodeMessageEnum codeMessage, Serializable... args) {
        super();
        this.codeMessage = codeMessage;
        this.args = args.clone();
    }
}
