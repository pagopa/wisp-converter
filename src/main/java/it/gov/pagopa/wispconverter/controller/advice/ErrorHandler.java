package it.gov.pagopa.wispconverter.controller.advice;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import it.gov.pagopa.wispconverter.controller.advice.model.ApiErrorResponse;
import it.gov.pagopa.wispconverter.exception.AppClientException;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.util.AppErrorUtil;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * All Exceptions are handled by this class
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ErrorHandler extends ResponseEntityExceptionHandler {

    private final AppErrorUtil appErrorUtil;

    @ApiResponses(value = {
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = {@ExampleObject(
                            """
                                    {
                                        "errorId": "68ce8c6a-6d53-486c-97fe-79430d24fb7d",
                                        "timestamp": "2023-10-09T08:01:39.421224Z",
                                        "httpStatusCode": 500,
                                        "httpStatusDescription": "Internal Server Error",
                                        "appErrorCode": "WIC-0500",
                                        "message": "An unexpected error has occurred. Please contact support"
                                    }
                                    """
                    )})
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = {@ExampleObject(
                            """
                                    {
                                        "timestamp": "2023-10-09T07:53:14.077792Z",
                                        "httpStatusCode": 400,
                                        "httpStatusDescription": "Bad Request",
                                        "appErrorCode": "WIC-0400",
                                        "message": "Bad request",
                                        "errors": [
                                            {
                                                "message": "Field error in ..."
                                            }
                                        ]
                                    }
                                    """
                    )})
            }),
            @ApiResponse(responseCode = "404", description = "Not found", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = {@ExampleObject(
                            """
                                    {
                                        "timestamp": "2023-10-09T07:53:43.367312Z",
                                        "httpStatusCode": 404,
                                        "httpStatusDescription": "Not Found",
                                        "appErrorCode": "WIC-0404",
                                        "message": "Request POST /api/v1/..... not found"
                                    }
                                    """
                    )})
            })
    })
    @ExceptionHandler({AppException.class, AppClientException.class})
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException appEx) {
        Pair<HttpStatus, ApiErrorResponse> httpStatusApiErrorResponsePair = appErrorUtil.buildApiErrorResponse(appEx, null, null);
        return ResponseEntity.status(httpStatusApiErrorResponsePair.getLeft())
                .body(httpStatusApiErrorResponsePair.getRight());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiErrorResponse.ErrorMessage> errorMessages = ex.getBindingResult().getAllErrors().stream()
                .map(oe -> ApiErrorResponse.ErrorMessage.builder().message(oe.toString()).build())
                .collect(Collectors.toList());
        AppException appEx = new AppException(ex, AppErrorCodeMessageEnum.BAD_REQUEST);
        Pair<HttpStatus, ApiErrorResponse> httpStatusApiErrorResponsePair = appErrorUtil.buildApiErrorResponse(appEx, null, errorMessages);
        return ResponseEntity.status(httpStatusApiErrorResponsePair.getLeft())
                .body(httpStatusApiErrorResponsePair.getRight());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        log.error(String.format("ExceptionHandler: ErrorId=[%s] %s", errorId, ex.getMessage()), ex);

        AppException appEx = new AppException(ex, AppErrorCodeMessageEnum.ERROR);
        // errorId viene usato solo per i casi di eccezioni non gestite
        Pair<HttpStatus, ApiErrorResponse> httpStatusApiErrorResponsePair = appErrorUtil.buildApiErrorResponse(appEx, errorId, null);
        return ResponseEntity.status(httpStatusApiErrorResponsePair.getLeft())
                .body(httpStatusApiErrorResponsePair.getRight());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public final ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<ApiErrorResponse.ErrorMessage> errorMessages = ex.getConstraintViolations().stream()
                .map(oe -> ApiErrorResponse.ErrorMessage.builder().message(oe.getMessage()).build())
                .collect(Collectors.toList());
        AppException appEx = new AppException(ex, AppErrorCodeMessageEnum.BAD_REQUEST);
        Pair<HttpStatus, ApiErrorResponse> httpStatusApiErrorResponsePair = appErrorUtil.buildApiErrorResponse(appEx, null, errorMessages);
        return ResponseEntity.status(httpStatusApiErrorResponsePair.getLeft())
                .body(httpStatusApiErrorResponsePair.getRight());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiErrorResponse.ErrorMessage> errorMessages = Stream.of(ex.getCause().getMessage())
                .map(oe -> ApiErrorResponse.ErrorMessage.builder().message(oe).build())
                .collect(Collectors.toList());
        AppException appEx = new AppException(ex, AppErrorCodeMessageEnum.BAD_REQUEST);
        Pair<HttpStatus, ApiErrorResponse> httpStatusApiErrorResponsePair = appErrorUtil.buildApiErrorResponse(appEx, null, errorMessages);
        return ResponseEntity.status(httpStatusApiErrorResponsePair.getLeft())
                .body(httpStatusApiErrorResponsePair.getRight());
    }

}
