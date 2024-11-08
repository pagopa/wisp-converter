package it.gov.pagopa.wispconverter.util.client;

import it.gov.pagopa.wispconverter.repository.model.enumz.OutcomeEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.service.model.re.ReRequestContext;
import it.gov.pagopa.wispconverter.service.model.re.ReResponseContext;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

@Slf4j
public abstract class AbstractAppClientLoggingInterceptor implements ClientHttpRequestInterceptor {

  public static final String REQUEST_DEFAULT_MESSAGE_PREFIX =
      "===> CLIENT Request OPERATION_ID=%s, CLIENT_OPERATION_ID=%s - ";
  public static final String RESPONSE_DEFAULT_MESSAGE_PREFIX =
      "<=== CLIENT Response OPERATION_ID=%s, CLIENT_OPERATION_ID=%s -";
  private static final int REQUEST_DEFAULT_MAX_PAYLOAD_LENGTH = 50;
  private static final int RESPONSE_DEFAULT_MAX_PAYLOAD_LENGTH = 50;
  private static final String SPACE = " ";
  private static final String PRETTY_OUT = "\n===> *";
  private static final String PRETTY_IN = "\n<=== *";
  public static final String DELIMITER = "\", \"";
  private final ReService reService;
  protected ClientServiceEnum clientServiceEnum;
  private boolean requestIncludeHeaders;
  private boolean responseIncludeHeaders;
  private boolean requestIncludePayload;
  private boolean responseIncludePayload;
  private Predicate<String> requestHeaderPredicate;
  private Predicate<String> responseHeaderPredicate;
  private int requestMaxPayloadLength = REQUEST_DEFAULT_MAX_PAYLOAD_LENGTH;
  private int responseMaxPayloadLength = RESPONSE_DEFAULT_MAX_PAYLOAD_LENGTH;
  private boolean requestPretty;
  private boolean responsePretty;
  private boolean mustPersistEventOnRE;

  protected AbstractAppClientLoggingInterceptor(
      RequestResponseLoggingProperties clientLoggingProperties,
      ReService reService,
      ClientServiceEnum clientServiceEnum) {
    this.reService = reService;
    this.clientServiceEnum = clientServiceEnum;
    this.mustPersistEventOnRE = true;

    if (clientLoggingProperties != null) {
      RequestResponseLoggingProperties.Request request = clientLoggingProperties.getRequest();
      if (request != null) {
        this.requestIncludeHeaders = request.isIncludeHeaders();
        this.requestIncludePayload = request.isIncludePayload();
        this.requestMaxPayloadLength =
            request.getMaxPayloadLength() != null
                ? request.getMaxPayloadLength()
                : REQUEST_DEFAULT_MAX_PAYLOAD_LENGTH;
        this.requestHeaderPredicate = s -> !s.equals(request.getMaskHeaderName());
        this.requestPretty = request.isPretty();
      }
      RequestResponseLoggingProperties.Response response = clientLoggingProperties.getResponse();
      if (response != null) {
        this.responseIncludeHeaders = response.isIncludeHeaders();
        this.responseIncludePayload = response.isIncludePayload();
        this.responseMaxPayloadLength =
            response.getMaxPayloadLength() != null
                ? response.getMaxPayloadLength()
                : RESPONSE_DEFAULT_MAX_PAYLOAD_LENGTH;
        this.responseHeaderPredicate = null;
        this.responsePretty = response.isPretty();
      }
    }
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    String startClient = String.valueOf(System.currentTimeMillis());
    String clientOperationId = UUID.randomUUID().toString();
    String operationId = MDC.get(Constants.MDC_OPERATION_ID);
    logRequest(clientOperationId, operationId, request, body);
    OutcomeEnum outcome;
    WorkflowStatus status = getOperationStatus(request.getURI().toString(), request.getMethod());

    ClientHttpResponse response = null;
    try {

      response = execution.execute(request, body);

      String executionClientTime = CommonUtility.getExecutionTime(startClient);
      MDC.put(Constants.MDC_CLIENT_EXECUTION_TIME, executionClientTime);
      if (response.getStatusCode().is2xxSuccessful()) {
        outcome = OutcomeEnum.OK;
      } else {
        outcome = OutcomeEnum.COMMUNICATION_RECEIVED_FAILURE;
      }
      logResponse(clientOperationId, operationId, executionClientTime, request, response);

      generateRe(request, body, response, status, outcome);

    } catch (Exception e) {

      outcome = OutcomeEnum.COMMUNICATION_RECEIVED_FAILURE;
      String executionClientTime = CommonUtility.getExecutionTime(startClient);
      MDC.put(Constants.MDC_CLIENT_EXECUTION_TIME, executionClientTime);
      logResponse(clientOperationId, operationId, executionClientTime, request, null);
      generateRe(request, body, response, status, outcome);
      throw e;

    } finally {

      MDC.remove(Constants.MDC_CLIENT_EXECUTION_TIME);
    }

    return response;
  }

  private void generateRe(
      HttpRequest request,
      byte[] body,
      ClientHttpResponse response,
      WorkflowStatus status,
      OutcomeEnum outcome)
      throws IOException {
    ReRequestContext requestContext = null;
    if (request != null) {
      requestContext =
          ReRequestContext.builder()
              .uri(request.getURI().toString())
              .method(request.getMethod())
              .headers(request.getHeaders())
              .payload(Arrays.toString(body))
              .build();
    }
    ReResponseContext responseContext = null;
    if (response != null) {
      responseContext =
          ReResponseContext.builder()
              .statusCode(HttpStatus.valueOf(response.getStatusCode().value()))
              .headers(response.getHeaders())
              .payload(StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8))
              .build();
    }
    reService.sendEvent(status, null, null, outcome, requestContext, responseContext);
  }

  protected String createRequestMessage(
      String clientOperationId, String operationId, HttpRequest request, @Nullable byte[] reqBody) {

    StringBuilder msg = new StringBuilder();
    appendRequestPrefix(msg, operationId, clientOperationId);
    appendRequestPath(msg, request);
    appendRequestHeaders(msg, request);
    appendRequestPayload(msg, reqBody);

    return msg.toString();
  }

  // Append the request prefix information
  private void appendRequestPrefix(
      StringBuilder msg, String operationId, String clientOperationId) {
    msg.append(String.format(REQUEST_DEFAULT_MESSAGE_PREFIX, operationId, clientOperationId));
    appendPretty(msg);
  }

  // Append the request path and method
  private void appendRequestPath(StringBuilder msg, HttpRequest request) {
    msg.append("path: ").append(request.getMethod()).append(' ').append(request.getURI());
  }

  // Append the request headers if needed
  private void appendRequestHeaders(StringBuilder msg, HttpRequest request) {
    if (!this.requestIncludeHeaders) return;

    HttpHeaders headers = collectHeaders(request);
    maskSensitiveHeaders(headers);

    String formattedHeaders = formatRequestHeaders(headers);
    if (formattedHeaders != null) {
      appendPretty(msg);
      msg.append("headers: [").append(formattedHeaders).append("]");
    }
  }

  // Collect headers from the request
  private HttpHeaders collectHeaders(HttpRequest request) {
    HttpHeaders headers = new HttpHeaders();
    request.getHeaders().forEach((key, values) -> headers.add(key, StringUtils.join(values, ",")));
    return headers;
  }

  // Mask headers based on the predicate
  private void maskSensitiveHeaders(HttpHeaders headers) {
    if (this.requestHeaderPredicate != null) {
      headers.forEach(
          (key, value) -> {
            if (!this.requestHeaderPredicate.test(key)) {
              headers.set(key, "masked");
            }
          });
    }
  }

  // Append the request payload if needed
  private void appendRequestPayload(StringBuilder msg, @Nullable byte[] reqBody) {
    if (!this.requestIncludePayload || reqBody == null) return;

    String payload = new String(reqBody, StandardCharsets.UTF_8);
    appendPretty(msg);
    msg.append("payload: ").append(payload);
  }

  // Adds pretty output if enabled
  private void appendPretty(StringBuilder msg) {
    if (this.requestPretty) {
      msg.append(PRETTY_OUT).append(SPACE);
    } else {
      msg.append(", ");
    }
  }

  protected String createResponseMessage(
      String clientOperationId,
      String operationId,
      String clientExecutionTime,
      HttpRequest request,
      ClientHttpResponse response)
      throws IOException {

    StringBuilder msg = new StringBuilder();
    appendResponsePrefix(msg, operationId, clientOperationId);
    appendRequestPath(msg, request);
    appendClientExecutionTime(msg, clientExecutionTime);
    appendResponseStatus(msg, response);
    appendResponseHeaders(msg, response);
    appendResponsePayload(msg, response);

    return msg.toString();
  }

  // Append the response prefix information
  private void appendResponsePrefix(
      StringBuilder msg, String operationId, String clientOperationId) {
    msg.append(String.format(RESPONSE_DEFAULT_MESSAGE_PREFIX, operationId, clientOperationId));
    appendPrettyIn(msg);
  }

  // Append the client execution time
  private void appendClientExecutionTime(StringBuilder msg, String clientExecutionTime) {
    appendPrettyIn(msg);
    msg.append("client-execution-time: ").append(clientExecutionTime).append("ms");
  }

  // Append the response status code if the response is not null
  private void appendResponseStatus(StringBuilder msg, ClientHttpResponse response)
      throws IOException {
    if (response != null) {
      appendPrettyIn(msg);
      msg.append("status: ").append(response.getStatusCode().value());
    } else {
      appendPrettyIn(msg);
      msg.append("NO RICEVUTA");
    }
  }

  // Append the response headers if enabled
  private void appendResponseHeaders(StringBuilder msg, ClientHttpResponse response) {
    if (response == null || !this.responseIncludeHeaders) return;

    HttpHeaders headers = collectHeaders(response);
    maskSensitiveHeaders(headers);
    String formattedHeaders = formatResponseHeaders(headers);

    if (formattedHeaders != null) {
      appendPrettyIn(msg);
      msg.append("headers: [").append(formattedHeaders).append("]");
    }
  }

  // Collect headers from the response
  private HttpHeaders collectHeaders(ClientHttpResponse response) {
    HttpHeaders headers = new HttpHeaders();
    response.getHeaders().forEach((key, values) -> headers.add(key, StringUtils.join(values, ",")));
    return headers;
  }

  // Append the response payload if enabled
  private void appendResponsePayload(StringBuilder msg, ClientHttpResponse response)
      throws IOException {
    if (!this.responseIncludePayload || response == null) return;

    String payload = bodyToString(response.getBody());
    if (!payload.isBlank()) {
      appendPrettyIn(msg);
      msg.append("payload: ").append(payload);
    }
  }

  // Adds pretty formatting if enabled
  private void appendPrettyIn(StringBuilder msg) {
    if (this.responsePretty) {
      msg.append(PRETTY_IN).append(SPACE);
    } else {
      msg.append(", ");
    }
  }

  protected void logRequest(
      String clientOperationId, String operationId, HttpRequest request, byte[] reqBody) {
    if (log.isDebugEnabled()) {
      log.debug(createRequestMessage(clientOperationId, operationId, request, reqBody));
    }
  }

  @SneakyThrows
  protected void logResponse(
      String clientOperationId,
      String operationId,
      String clientExecutionTime,
      HttpRequest request,
      ClientHttpResponse response) {
    if (log.isDebugEnabled()) {
      log.debug(
          createResponseMessage(
              clientOperationId, operationId, clientExecutionTime, request, response));
    }
  }

  private String formatRequestHeaders(MultiValueMap<String, String> headers) {
    Stream<String> stream =
        headers.entrySet().stream()
            .map(
                entry -> {
                  String values =
                      entry.getValue().stream().collect(Collectors.joining(DELIMITER, "\"", "\""));
                  if (this.requestPretty) {
                    return PRETTY_OUT + "*\t" + entry.getKey() + ": [" + values + "]";
                  } else {
                    return entry.getKey() + ": [" + values + "]";
                  }
                });
    if (this.requestPretty) {
      return stream.collect(Collectors.joining(""));
    } else {
      return stream.collect(Collectors.joining(", "));
    }
  }

  private String formatResponseHeaders(MultiValueMap<String, String> headers) {
    Stream<String> stream =
        headers.entrySet().stream()
            .map(
                entry -> {
                  if (this.responsePretty) {
                    String values =
                        entry.getValue().stream()
                            .collect(Collectors.joining(DELIMITER, "\"", "\""));
                    return PRETTY_IN + "*\t" + entry.getKey().toLowerCase() + ": [" + values + "]";
                  } else {
                    String values =
                        entry.getValue().stream()
                            .collect(Collectors.joining(DELIMITER, "\"", "\""));
                    return entry.getKey().toLowerCase() + ": [" + values + "]";
                  }
                });
    if (this.requestPretty) {
      return stream.collect(Collectors.joining(""));
    } else {
      return stream.collect(Collectors.joining(", "));
    }
  }

  private String bodyToString(InputStream body) throws IOException {
    return StreamUtils.copyToString(body, StandardCharsets.UTF_8);
  }

  protected void avoidEventPersistenceOnRE() {
    this.mustPersistEventOnRE = false;
  }

  protected abstract WorkflowStatus getOperationStatus(String url, HttpMethod httpMethod);
}
