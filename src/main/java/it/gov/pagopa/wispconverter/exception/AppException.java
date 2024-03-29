package it.gov.pagopa.wispconverter.exception;

import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.aspect.LoggingAspect;
import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.util.Assert;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.time.Instant;

@Getter
public class AppException extends ErrorResponseException {

    private final AppErrorCodeMessageEnum error;
    private final transient Object[] args;
    public AppException(AppErrorCodeMessageEnum error, Object... args) {
        super(error.getStatus(), forAppErrorCodeMessageEnum(error), null, error.getMessageDetailCode(), getArgsOrNull(args));
        this.error = error;
        this.args = getArgsOrNull(args);
    }
    public AppException(Throwable cause, AppErrorCodeMessageEnum error, Object... args) {
        super(error.getStatus(), forAppErrorCodeMessageEnum(error), cause, error.getMessageDetailCode(), getArgsOrNull(args));
        this.error = error;
        this.args = getArgsOrNull(args);
    }

    private static Object[] getArgsOrNull(Object... args){
        return args.length > 0 ? args.clone() : null;
    }

    private static ProblemDetail forAppErrorCodeMessageEnum(AppErrorCodeMessageEnum error) {
        Assert.notNull(error, "AppErrorCodeMessageEnum is required");

        ProblemDetail problemDetail = ProblemDetail.forStatus(error.getStatus());
//        problemDetail.setType(URI.create("https://pagopa.it/errors/"+getAppCode(error)));
//        problemDetail.setTitle("error-code."+error.name()+".title");
//        problemDetail.setDetail("error-code."+error.name()+".detail");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error-code", getAppCode(error));
        String operationId = MDC.get(LoggingAspect.OPERATION_ID);
        if(operationId!=null){
            problemDetail.setProperty("operation-id", operationId);
        }
        return problemDetail;
    }

    private static String getAppCode(AppErrorCodeMessageEnum error){
        return String.format("%s-%s", Constants.SERVICE_CODE_APP, error.getCode());
    }


}
