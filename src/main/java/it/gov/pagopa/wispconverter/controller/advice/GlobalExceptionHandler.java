package it.gov.pagopa.wispconverter.controller.advice;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * All Exceptions are handled by this class
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Value("${error.code.uri}")
    private String errorCodeUri;
    private static final String ERROR_CODE_TITLE = "error-code.%s.title";
    private static final String ERROR_CODE_DETAIL = "error-code.%s.detail";

    private static final String EXTRA_FIELD_OPERATION_ID = "operation-id";
    private static final String EXTRA_FIELD_ERROR_TIMESTAMP = "timestamp";
    private static final String EXTRA_FIELD_ERROR_CODE = "error-code";

    private final MessageSource messageSource;

    @ExceptionHandler(AppException.class)
    public ErrorResponse handleAppException(AppException appEx) {
        return forAppException(appEx);
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGenericException(Exception ex) {
        String operationId = MDC.get(Constants.MDC_OPERATION_ID);
        log.error(String.format("GenericException: operation-id=[%s]", operationId!=null?operationId:"n/a"), ex);
        return forAppException(new AppException(ex, AppErrorCodeMessageEnum.ERROR, ex.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            ProblemDetail problemDetail = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
            setExtraProperties(problemDetail);
            setMDCError(statusCode, problemDetail);
            body = problemDetail;
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private void setMDCError(HttpStatusCode statusCode, ProblemDetail problemDetail){
        setMDCCloseOperation("KO", statusCode);

        MDC.put(Constants.MDC_ERROR_TITLE, problemDetail.getTitle());
        MDC.put(Constants.MDC_ERROR_DETAIL, problemDetail.getDetail());

        Map<String, Object> properties = problemDetail.getProperties();
        if (properties != null) {
            String errorCode = (String)properties.get(EXTRA_FIELD_ERROR_CODE);
            MDC.put(Constants.MDC_ERROR_CODE, errorCode);
        }
    }
    private void setMDCCloseOperation(String status, HttpStatusCode statusCode){
        MDC.put(Constants.MDC_STATUS, status);
        MDC.put(Constants.MDC_STATUS_CODE, String.valueOf(statusCode.value()));
        String executionTime = CommonUtility.getExecutionTime(MDC.get(Constants.MDC_START_TIME));
        MDC.put(Constants.MDC_EXECUTION_TIME, executionTime);
    }

    private ErrorResponse forAppException(AppException appEx){
        return ErrorResponse.builder(appEx, forAppErrorCodeMessageEnum(appEx.getError(), appEx.getMessage()))
                .titleMessageCode(String.format(ERROR_CODE_TITLE, appEx.getError().name()))
                .detailMessageCode(String.format(ERROR_CODE_DETAIL, appEx.getError().name()))
                .detailMessageArguments(appEx.getArgs())
                .build(messageSource, LocaleContextHolder.getLocale());
    }

    private ProblemDetail forAppErrorCodeMessageEnum(AppErrorCodeMessageEnum error, @Nullable String detail) {
        Assert.notNull(error, "AppErrorCodeMessageEnum is required");

        ProblemDetail problemDetail = ProblemDetail.forStatus(error.getStatus());
        problemDetail.setType(getTypeFromErrorCode(getAppCode(error)));
        problemDetail.setTitle(error.getTitle());
        problemDetail.setDetail(detail);

        problemDetail.setProperty(EXTRA_FIELD_ERROR_CODE, getAppCode(error));
        setExtraProperties(problemDetail);
        setMDCError(error.getStatus(), problemDetail);

        return problemDetail;
    }

    private void setExtraProperties(ProblemDetail problemDetail){
        problemDetail.setProperty(EXTRA_FIELD_ERROR_TIMESTAMP, Instant.now());
        String operationId = MDC.get(Constants.MDC_OPERATION_ID);
        if(operationId!=null){
            problemDetail.setProperty(EXTRA_FIELD_OPERATION_ID, operationId);
        }
    }
    private URI getTypeFromErrorCode(String errorCode) {
        return new DefaultUriBuilderFactory()
                .uriString(errorCodeUri)
                .pathSegment(errorCode)
                .build();
    }

    private String getAppCode(AppErrorCodeMessageEnum error){
        return String.format("%s-%s", Constants.SERVICE_CODE_APP, error.getCode());
    }


//
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
//                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = {@ExampleObject(
//                            """
//                                    {
//                                        "errorId": "68ce8c6a-6d53-486c-97fe-79430d24fb7d",
//                                        "timestamp": "2023-10-09T08:01:39.421224Z",
//                                        "httpStatusCode": 500,
//                                        "httpStatusDescription": "Internal Server Error",
//                                        "appErrorCode": "WIC-0500",
//                                        "message": "An unexpected error has occurred. Please contact support"
//                                    }
//                                    """
//                    )})
//            }),
//            @ApiResponse(responseCode = "400", description = "Bad Request", content = {
//                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = {@ExampleObject(
//                            """
//                                    {
//                                        "timestamp": "2023-10-09T07:53:14.077792Z",
//                                        "httpStatusCode": 400,
//                                        "httpStatusDescription": "Bad Request",
//                                        "appErrorCode": "WIC-0400",
//                                        "message": "Bad request",
//                                        "errors": [
//                                            {
//                                                "message": "Field error in ..."
//                                            }
//                                        ]
//                                    }
//                                    """
//                    )})
//            }),
//            @ApiResponse(responseCode = "404", description = "Not found", content = {
//                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = {@ExampleObject(
//                            """
//                                    {
//                                        "timestamp": "2023-10-09T07:53:43.367312Z",
//                                        "httpStatusCode": 404,
//                                        "httpStatusDescription": "Not Found",
//                                        "appErrorCode": "WIC-0404",
//                                        "message": "Request POST /api/v1/..... not found"
//                                    }
//                                    """
//                    )})
//            })
//    })
//    @ExceptionHandler({AppException.class, AppClientException.class})
//    public ResponseEntity<ApiErrorResponse> handleAppException(AppException appEx) {
//        Pair<HttpStatus, ApiErrorResponse> httpStatusApiErrorResponsePair = appErrorUtil.buildApiErrorResponse(appEx, null, null);
//        return ResponseEntity.status(httpStatusApiErrorResponsePair.getLeft())
//                .body(httpStatusApiErrorResponsePair.getRight());
//    }
//
//    @Override
//    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
//        List<ApiErrorResponse.ErrorMessage> errorMessages = ex.getBindingResult().getAllErrors().stream()
//                .map(oe -> ApiErrorResponse.ErrorMessage.builder().message(oe.toString()).build())
//                .collect(Collectors.toList());
//        AppException appEx = new AppException(ex, AppErrorCodeMessageEnum.BAD_REQUEST);
//        Pair<HttpStatus, ApiErrorResponse> httpStatusApiErrorResponsePair = appErrorUtil.buildApiErrorResponse(appEx, null, errorMessages);
//        return ResponseEntity.status(httpStatusApiErrorResponsePair.getLeft())
//                .body(httpStatusApiErrorResponsePair.getRight());
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, WebRequest request) {
//        String errorId = UUID.randomUUID().toString();
//        log.error(String.format("ExceptionHandler: ErrorId=[%s] %s", errorId, ex.getMessage()), ex);
//
//        AppException appEx = new AppException(ex, AppErrorCodeMessageEnum.ERROR);
//        // errorId viene usato solo per i casi di eccezioni non gestite
//        Pair<HttpStatus, ApiErrorResponse> httpStatusApiErrorResponsePair = appErrorUtil.buildApiErrorResponse(appEx, errorId, null);
//        return ResponseEntity.status(httpStatusApiErrorResponsePair.getLeft())
//                .body(httpStatusApiErrorResponsePair.getRight());
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public final ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
//        List<ApiErrorResponse.ErrorMessage> errorMessages = ex.getConstraintViolations().stream()
//                .map(oe -> ApiErrorResponse.ErrorMessage.builder().message(oe.getMessage()).build())
//                .collect(Collectors.toList());
//        AppException appEx = new AppException(ex, AppErrorCodeMessageEnum.BAD_REQUEST);
//        Pair<HttpStatus, ApiErrorResponse> httpStatusApiErrorResponsePair = appErrorUtil.buildApiErrorResponse(appEx, null, errorMessages);
//        return ResponseEntity.status(httpStatusApiErrorResponsePair.getLeft())
//                .body(httpStatusApiErrorResponsePair.getRight());
//    }
//
//    @Override
//    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
//        List<ApiErrorResponse.ErrorMessage> errorMessages = Stream.of(ex.getCause().getMessage())
//                .map(oe -> ApiErrorResponse.ErrorMessage.builder().message(oe).build())
//                .collect(Collectors.toList());
//        AppException appEx = new AppException(ex, AppErrorCodeMessageEnum.BAD_REQUEST);
//        Pair<HttpStatus, ApiErrorResponse> httpStatusApiErrorResponsePair = appErrorUtil.buildApiErrorResponse(appEx, null, errorMessages);
//        return ResponseEntity.status(httpStatusApiErrorResponsePair.getLeft())
//                .body(httpStatusApiErrorResponsePair.getRight());
//    }

}
