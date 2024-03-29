package it.gov.pagopa.wispconverter.exception;

import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.aspect.LoggingAspect;
import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.util.Assert;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;

@Getter
public class AppException extends ErrorResponseException {

    private final AppErrorCodeMessageEnum error;
    private final transient Object[] args;
    private String reason;

    public AppException(AppErrorCodeMessageEnum error, String reason, Object... args) {
        super(error.getStatus(), forAppErrorCodeMessageEnum(error, reason), null, null, getArgsOrNull(args));
        this.error = error;
        this.reason = reason;
        this.args = getArgsOrNull(args);
    }
    public AppException(Throwable cause, AppErrorCodeMessageEnum error, String reason, Object... args) {
        super(error.getStatus(), forAppErrorCodeMessageEnum(error, reason), cause, null, getArgsOrNull(args));
        this.error = error;
        this.reason = reason;
        this.args = getArgsOrNull(args);
    }

    private static Object[] getArgsOrNull(Object... args){
        return args.length > 0 ? args.clone() : null;
    }

    private static ProblemDetail forAppErrorCodeMessageEnum(AppErrorCodeMessageEnum error, String reason) {
        Assert.notNull(error, "AppErrorCodeMessageEnum is required");

        ProblemDetail problemDetail = ProblemDetail.forStatus(error.getStatus());
        problemDetail.setType(URI.create("https://pagopa.it/errors/"+getAppCode(error)));
        problemDetail.setTitle(error.getTitle());
        problemDetail.setDetail(reason);

        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error-code", getAppCode(error));
        String operationId = MDC.get(LoggingAspect.OPERATION_ID);
        if(operationId!=null){
            problemDetail.setProperty("operation-id", operationId);
        }
        return problemDetail;
    }

    @Override
    public String getTitleMessageCode() {
        return "error-code."+error.name()+".title";
    }

    @Override
    public String getDetailMessageCode() {
        return "error-code."+error.name()+".detail";
    }

    @Override
    public String getTypeMessageCode() {
        return URI.create("https://pagopa.it/errors/"+getAppCode(error)).toString();
    }


    private static String getAppCode(AppErrorCodeMessageEnum error){
        return String.format("%s-%s", Constants.SERVICE_CODE_APP, error.getCode());
    }

//    @Override
//    public ProblemDetail updateAndGetBody(MessageSource messageSource, Locale locale) {
//        if (messageSource != null) {
//            String type = messageSource.getMessage(getBody().getType().toString(), null, null, locale);
//            if (type != null) {
//                getBody().setType(URI.create(type));
//            }
//            Object[] arguments = getDetailMessageArguments(messageSource, locale);
//            if(getBody().getDetail() != null){
//                String detail = messageSource.getMessage(getBody().getDetail(), arguments, null, locale);
//                if (detail != null) {
//                    getBody().setDetail(detail);
//                }
//            }
//
//            String title = messageSource.getMessage(getTitleMessageCode(), null, null, locale);
//            if (title != null) {
//                getBody().setTitle(title);
//            }
//        }
//        return getBody();
//    }
}
