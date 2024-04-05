package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.ErrorResponse;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ErrorUtil {

    public static final String EXTRA_FIELD_OPERATION_ID = "operation-id";
    public static final String EXTRA_FIELD_ERROR_TIMESTAMP = "timestamp";
    public static final String EXTRA_FIELD_ERROR_CODE = "error-code";
    private static final String ERROR_CODE_TITLE = "error-code.%s.title";
    private static final String ERROR_CODE_DETAIL = "error-code.%s.detail";
    private final MessageSource messageSource;
    @Value("${exception.error-code.uri}")
    private String errorCodeUri;

    public ErrorResponse forAppException(AppException appEx) {
        return ErrorResponse.builder(appEx, forAppErrorCodeMessageEnum(appEx.getError(), appEx.getMessage()))
                .titleMessageCode(String.format(ERROR_CODE_TITLE, appEx.getError().name()))
                .detailMessageCode(String.format(ERROR_CODE_DETAIL, appEx.getError().name()))
                .detailMessageArguments(appEx.getArgs())
                .build();
    }

    public ProblemDetail forAppErrorCodeMessageEnum(AppErrorCodeMessageEnum error, @Nullable String detail) {
        Assert.notNull(error, "AppErrorCodeMessageEnum is required");

        ProblemDetail problemDetail = ProblemDetail.forStatus(error.getStatus());
        problemDetail.setType(getTypeFromErrorCode(getAppCode(error)));
        problemDetail.setTitle(error.getTitle());
        problemDetail.setDetail(detail);

        problemDetail.setProperty(EXTRA_FIELD_ERROR_CODE, getAppCode(error));

        return problemDetail;
    }

    private void setExtraProperties(ProblemDetail problemDetail) {
        problemDetail.setProperty(EXTRA_FIELD_ERROR_TIMESTAMP, Instant.now());
        String operationId = MDC.get(Constants.MDC_OPERATION_ID);
        if (operationId != null) {
            problemDetail.setProperty(EXTRA_FIELD_OPERATION_ID, operationId);
        }
    }

    public void finalizeError(ProblemDetail problemDetail, int statusCode) {
        setExtraProperties(problemDetail);
        MDCUtil.setMDCError(problemDetail);
        MDCUtil.setMDCCloseFailedOperation(statusCode);
    }

    public URI getTypeFromErrorCode(String errorCode) {
        return new DefaultUriBuilderFactory()
                .uriString(errorCodeUri)
                .pathSegment(errorCode)
                .build();
    }

    public String getAppCode(AppErrorCodeMessageEnum error) {
        return String.format("%s-%s", Constants.SERVICE_CODE_APP, error.getCode());
    }
}
