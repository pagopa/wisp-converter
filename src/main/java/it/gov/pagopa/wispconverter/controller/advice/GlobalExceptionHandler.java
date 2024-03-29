package it.gov.pagopa.wispconverter.controller.advice;

import it.gov.pagopa.wispconverter.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;

/**
 * All Exceptions are handled by this class
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(ErrorResponseException.class)
    ProblemDetail handleAppException(ErrorResponseException e) {
        Locale locale = LocaleContextHolder.getLocale();
        String ff = messageSource.getMessage("error-code.PERSISTENCE_.title", Arrays.asList("ddd").toArray(), locale);
//        return e.getBody();
        return e.getBody();
    }
//
//    private static ProblemDetail forAppException(AppException e) {
//        Assert.notNull(e, "AppException is required");
//        AppErrorCodeMessageEnum error = e.getError();
//
//        ProblemDetail problemDetail = e.getBody();
//        problemDetail.setType(URI.create("https://pagopa.it/errors/"+getAppCode(error)));
//        problemDetail.setTitle("error-code."+error.name()+".title");
//        problemDetail.setDetail("error-code."+error.name()+".detail");
//        problemDetail.setProperty("timestamp", Instant.now());
//        problemDetail.setProperty("error-code", getAppCode(error));
//        String operationId = MDC.get(OPERATION_ID);
//        if(operationId!=null){
//            problemDetail.setProperty("operation-id", operationId);
//        }
//        return problemDetail;
//    }
//
//    private static String getAppCode(AppErrorCodeMessageEnum error){
//        return String.format("%s-%s", Constants.SERVICE_CODE_APP, error.getCode());
//    }

//    private ErrorResponse forAppClientException(AppClientException e) {
//        Assert.notNull(e, "AppClientException is required");
//        AppErrorCodeMessageEnum codeMessage = e.getError();
//
//        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(codeMessage.getStatus(), codeMessage.getReason());
//        problemDetail.setProperty("timestamp", Instant.now());
//        problemDetail.setProperty("client-error-code", String.format("%s-%s", Constants.SERVICE_CODE_APP, codeMessage.getCode()));
//
//        Optional<String> requestIdOpt = Optional.ofNullable(MDC.get(HEADER_REQUEST_ID));
//        requestIdOpt.ifPresent(requestId -> problemDetail.setProperty(HEADER_REQUEST_ID.toLowerCase(), requestId));
//
//        String errorId = UUID.randomUUID().toString();
//        problemDetail.setProperty("error-id", errorId);
//
//        return ErrorResponse.builder(e, problemDetail)
//                .detailMessageCode(codeMessage.getMessageDetailCode())
//                .detailMessageArguments(e.getArgs())
//                .build(messageSource, LocaleContextHolder.getLocale());
//    }


    //    private final AppErrorUtil appErrorUtil;
//    private final MessageSource messageSource;
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
