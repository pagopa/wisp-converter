package it.gov.pagopa.wispconverter.controller.advice;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ErrorUtil;
import it.gov.pagopa.wispconverter.util.MDCUtil;
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

    private final ErrorUtil errorUtil;
    private final MessageSource messageSource;

    @ExceptionHandler(AppException.class)
    public ErrorResponse handleAppException(AppException appEx) {
        ErrorResponse errorResponse = errorUtil.forAppException(appEx);
        ProblemDetail problemDetail = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
        MDCUtil.setMDCError(problemDetail);
        MDCUtil.setMDCCloseFailedOperation(errorResponse.getStatusCode().value());
        return errorResponse;
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGenericException(Exception ex, WebRequest request) {
        String operationId = MDC.get(Constants.MDC_OPERATION_ID);
        log.error(String.format("GenericException: operation-id=[%s]", operationId!=null?operationId:"n/a"), ex);
        ErrorResponse errorResponse = errorUtil.forAppException(new AppException(ex, AppErrorCodeMessageEnum.ERROR, ex.getMessage()));
        ProblemDetail problemDetail = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
        MDCUtil.setMDCError(problemDetail);
        MDCUtil.setMDCCloseFailedOperation(errorResponse.getStatusCode().value());
        return errorResponse;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            ProblemDetail problemDetail = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
            errorUtil.setExtraProperties(problemDetail);
            MDCUtil.setMDCError(problemDetail);
            MDCUtil.setMDCCloseFailedOperation(errorResponse.getStatusCode().value());
            body = problemDetail;
        } else {
            MDCUtil.setMDCCloseFailedOperation(statusCode.value());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
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
