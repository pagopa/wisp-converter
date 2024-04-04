package it.gov.pagopa.wispconverter.util.client;

import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.Constants;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
public abstract class AbstractAppClientLoggingInterceptor implements ClientHttpRequestInterceptor {

  public static final String REQUEST_DEFAULT_MESSAGE_PREFIX = "===> CLIENT Request OPERATION_ID=%s, CLIENT_OPERATION_ID=%s - ";
  public static final String RESPONSE_DEFAULT_MESSAGE_PREFIX = "<=== CLIENT Response OPERATION_ID=%s, CLIENT_OPERATION_ID=%s -";
  private static final int REQUEST_DEFAULT_MAX_PAYLOAD_LENGTH = 50;
  private static final int RESPONSE_DEFAULT_MAX_PAYLOAD_LENGTH = 50;

  private static final String SPACE = " ";
  private static final String PRETTY_OUT = "\n===> *";
  private static final String PRETTY_IN = "\n<=== *";

  private boolean requestIncludeHeaders = false;

  private boolean responseIncludeHeaders = false;

  private boolean requestIncludePayload = false;

  private boolean responseIncludePayload = false;

  private Predicate<String> requestHeaderPredicate;

  private Predicate<String> responseHeaderPredicate;

  private int requestMaxPayloadLength = REQUEST_DEFAULT_MAX_PAYLOAD_LENGTH;

  private int responseMaxPayloadLength = RESPONSE_DEFAULT_MAX_PAYLOAD_LENGTH;

  private boolean requestPretty = false;

  private boolean responsePretty = false;

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    String startClient = String.valueOf(System.currentTimeMillis());
    String clientOperationId = UUID.randomUUID().toString();
    MDC.put(Constants.MDC_CLIENT_OPERATION_ID, clientOperationId);
    String operationId = MDC.get(Constants.MDC_OPERATION_ID);

    request(clientOperationId, operationId, request, body);
    ClientHttpResponse response = execution.execute(request, body);

    String executionClientTime = CommonUtility.getExecutionTime(startClient);
    MDC.put(Constants.MDC_CLIENT_EXECUTION_TIME, executionClientTime);

    response(clientOperationId, operationId, executionClientTime, request, response);
    return response;
  }


  public String createRequestMessage(String clientOperationId, String operationId, HttpRequest request, byte[] reqBody) {
    StringBuilder msg = new StringBuilder();
    msg.append(String.format(REQUEST_DEFAULT_MESSAGE_PREFIX, operationId, clientOperationId));
    if(isRequestPretty()){
      msg.append(PRETTY_OUT).append(SPACE);
    }
    msg.append("path: ").append(request.getMethod()).append(' ');
    msg.append(request.getURI());

    if (isRequestIncludeHeaders()) {
      HttpHeaders headers = new HttpHeaders();
      request.getHeaders().forEach((s,h)->{
        headers.add(s, StringUtils.join(h,","));
      });
      if (getRequestHeaderPredicate() != null) {
        headers.forEach(
            (key, value) -> {
              if (!getRequestHeaderPredicate().test(key)) {
                headers.set(key, "masked");
              }
            });
      }
      String formatRequestHeaders = formatRequestHeaders(headers);
      if(formatRequestHeaders!=null){
        if(isRequestPretty()){
          msg.append(PRETTY_OUT).append(SPACE);
        } else{
          msg.append(", ");
        }
        msg.append("headers: ").append(formatRequestHeaders);
      }
    }

    if (isRequestIncludePayload()) {
      String payload = new String(reqBody, StandardCharsets.UTF_8);
      if (payload != null) {
        if(isRequestPretty()){
          msg.append(PRETTY_OUT).append(SPACE);
        } else{
          msg.append(", ");
        }
        msg.append("payload: ").append(payload);
      }
    }

    return msg.toString();
  }

  public String createResponseMessage(String clientOperationId, String operationId, String clientExecutionTime, HttpRequest request, ClientHttpResponse response)
      throws IOException {
    StringBuilder msg = new StringBuilder();
    msg.append(String.format(RESPONSE_DEFAULT_MESSAGE_PREFIX, operationId, clientOperationId));
    if(isResponsePretty()){
      msg.append(PRETTY_IN).append(SPACE);
    }
    msg.append("path: ").append(request.getMethod()).append(' ');
    msg.append(request.getURI());

    if(isResponsePretty()){
      msg.append(PRETTY_IN).append(SPACE);
    }
    msg.append("status: ").append(response.getStatusCode().value());

    if(isResponsePretty()){
      msg.append(PRETTY_IN).append(SPACE);
    } else{
      msg.append(", ");
    }
    msg.append("client-execution-time: ").append(clientExecutionTime).append("ms");

    if (isResponseIncludeHeaders()) {
      HttpHeaders headers = new HttpHeaders();
      response.getHeaders().forEach((s,h)->{
        headers.add(s, StringUtils.join(h,","));
      });
      if (getResponseHeaderPredicate() != null) {
        headers.forEach(
            (key, value) -> {
              if (!getRequestHeaderPredicate().test(key)) {
                headers.set(key, "masked");
              }
            });
      }
      String formatResponseHeaders = formatResponseHeaders(headers);
      if(formatResponseHeaders!=null){
        if(isRequestPretty()){
          msg.append(PRETTY_IN).append(SPACE);
        } else{
          msg.append(", ");
        }
        msg.append("headers: ").append(formatResponseHeaders);
      }
    }

    if (isResponseIncludePayload()) {
      String payload = bodyToString(response.getBody());
      if (!payload.isBlank()) {
        if(isRequestPretty()){
          msg.append(PRETTY_IN).append(SPACE);
        } else{
          msg.append(", ");
        }
        msg.append("payload: ").append(payload);
      }
    }

    return msg.toString();
  }



  protected abstract void request(String clientOperationId, String operationId, HttpRequest request, byte[] reqBody);

  protected abstract void response(String clientOperationId, String operationId, String clientExecutionTime, HttpRequest request, ClientHttpResponse response);

  private String formatRequestHeaders(MultiValueMap<String, String> headers) {
    Stream<String> stream = headers.entrySet().stream()
            .map((entry) -> {
              if(isRequestPretty()){
                String values = entry.getValue().stream().collect(Collectors.joining("\", \"","\"","\""));
                return PRETTY_OUT +"*\t"+entry.getKey() + ": [" + values + "]";
              } else {
                String values = entry.getValue().stream().collect(Collectors.joining("\", \"","\"","\""));
                return entry.getKey() + ": [" + values + "]";
              }
            });
    if(isRequestPretty()){
      return stream.collect(Collectors.joining(""));
    } else {
      return stream.collect(Collectors.joining(", "));
    }
  }

  private String formatResponseHeaders(MultiValueMap<String, String> headers) {
    Stream<String> stream = headers.entrySet().stream()
            .map((entry) -> {
              if(isResponsePretty()){
                String values = entry.getValue().stream().collect(Collectors.joining("\", \"","\"","\""));
                return PRETTY_IN +"*\t"+entry.getKey().toLowerCase() + ": [" + values + "]";
              } else {
                String values = entry.getValue().stream().collect(Collectors.joining("\", \"","\"","\""));
                return entry.getKey().toLowerCase() + ": [" + values + "]";
              }
            });
    if(isRequestPretty()){
      return stream.collect(Collectors.joining(""));
    } else {
      return stream.collect(Collectors.joining(", "));
    }
  }

  private String bodyToString(InputStream body) throws IOException {
    StringBuilder builder = new StringBuilder();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8));
    String line = bufferedReader.readLine();
    while (line != null) {
      builder.append(line).append(System.lineSeparator());
      line = bufferedReader.readLine();
    }
    bufferedReader.close();
    return builder.toString();
  }

}
