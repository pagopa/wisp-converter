package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

@Getter
public class AppException extends RuntimeException {

    private final AppErrorCodeMessageEnum error;
    private final transient Object[] args;
//    private final String detail;

    public AppException(AppErrorCodeMessageEnum error, Object... args) {
        super(composeMessage(error, getArgsOrNull(args)));
        this.error = error;
        this.args = getArgsOrNull(args);
//        this.detail = composeMessage(error, getArgsOrNull(args));
    }

    public AppException(Throwable cause, AppErrorCodeMessageEnum error, Object... args) {
        super(composeMessage(error, getArgsOrNull(args)), cause);
        this.error = error;
        this.args = getArgsOrNull(args);
//        this.detail = composeMessage(error, getArgsOrNull(args));
    }

    private static Object[] getArgsOrNull(Object... args) {
        return args.length > 0 ? args.clone() : null;
    }

    private static String composeMessage(AppErrorCodeMessageEnum error, Object[] args){
        if(error.getDetail() != null){
            return MessageFormat.format(error.getDetail(), args);
        }
        return null;
    }

}