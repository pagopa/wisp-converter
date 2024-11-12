package it.gov.pagopa.wispconverter.exception;

import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ErrorUtil;
import it.gov.pagopa.wispconverter.util.MDCUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * All Exceptions are handled by this class
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ErrorUtil errorUtil;
    private final MessageSource messageSource;

    @ExceptionHandler(AppException.class)
    public ErrorResponse handleAppException(AppException appEx) {
        String operationId = MDC.get(Constants.MDC_OPERATION_ID);

        if (appEx.getError() == AppErrorCodeMessageEnum.ERROR || appEx.getError() == AppErrorCodeMessageEnum.GENERIC_ERROR) {
            log.error(String.format("[ALERT] AppException: operation-id=[%s]", operationId != null ? operationId : "n/a"), appEx);
        } else {
            log.error(String.format("AppException: operation-id=[%s]", operationId != null ? operationId : "n/a"), appEx);
        }

        ErrorResponse errorResponse = errorUtil.forAppException(appEx);
        ProblemDetail problemDetail = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
        errorUtil.finalizeError(appEx, problemDetail, errorResponse.getStatusCode().value());

        log.error("Failed API operation {} - error: {}", MDC.get(Constants.MDC_BUSINESS_PROCESS), errorResponse);

        return errorResponse;
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGenericException(Exception ex) {
        String operationId = MDC.get(Constants.MDC_OPERATION_ID);
        log.error(String.format("GenericException: operation-id=[%s]", operationId != null ? operationId : "n/a"), ex);

        AppException appEx = new AppException(ex, AppErrorCodeMessageEnum.ERROR, ex.getMessage());
        ErrorResponse errorResponse = errorUtil.forAppException(appEx);
        ProblemDetail problemDetail = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
        errorUtil.finalizeError(ex, problemDetail, errorResponse.getStatusCode().value());

        log.error("Failed API operation {} - error: {}", MDC.get(Constants.MDC_BUSINESS_PROCESS), errorResponse);

        return errorResponse;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            ProblemDetail problemDetail = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
            errorUtil.finalizeError(ex, problemDetail, errorResponse.getStatusCode().value());

            body = problemDetail;
        } else {
            MDCUtil.setMDCCloseFailedOperation(statusCode.value());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

}
