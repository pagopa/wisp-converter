package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.util.filter.RepeatableContentCachingRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

@Slf4j
public class ReUtil {

  private static final String UNZIP_ERROR = "Unzip error";

  //    @Deprecated
  //    public static ReEventDto.ReEventDtoBuilder getREBuilder() {
  //        return createBaseRE()
  //                .eventCategory(EventCategoryEnum.INTERNAL);
  //        //.eventSubcategory(EventSubcategoryEnum.INTERN);
  //    }
  //
  //    public static ReEventDto createEventForEndpoint(HttpServletRequest request,
  // HttpServletResponse response) {
  //        String httpMethod = request.getMethod();
  //
  //        StringBuilder requestUriBuilder = new StringBuilder(request.getRequestURI());
  //        String queryString = request.getQueryString();
  //        if (queryString != null) {
  //            requestUriBuilder.append('?').append(queryString);
  //        }
  //        String httpUri = requestUriBuilder.toString();
  //
  //        String requestHeaders = formatServerRequestHeaders(request);
  //        String responseHeaders = formatServerResponseHeaders(response);
  //        int httpStatusCode = response.getStatus();
  //        String executionTimeMs = MDC.get(Constants.MDC_EXECUTION_TIME);
  //
  //        String requestPayload = null;
  //        String responsePayload = null;
  //        try {
  //            String plainRequestPayload = getRequestMessagePayload(request);
  //            if (plainRequestPayload != null) {
  //                requestPayload = AppBase64Util.base64Encode(ZipUtil.zip(plainRequestPayload));
  //            }
  //            String plainResponsePayload = getResponseMessagePayload(response);
  //            if (plainResponsePayload != null) {
  //                responsePayload = AppBase64Util.base64Encode(ZipUtil.zip(plainResponsePayload));
  //            }
  //        } catch (IOException e) {
  //            log.error(UNZIP_ERROR, e);
  //        }
  //
  //        return createBaseRE()
  //                .eventCategory(EventCategoryEnum.INTERFACE)
  //                .status(MDC.get(MDC_STATUS))
  //                .requestPayload(requestPayload)
  //                .responsePayload(responsePayload)
  //                .requestHeaders(requestHeaders)
  //                .responseHeaders(responseHeaders)
  //                .outcome(httpStatusCode > 399 ? "KO" : OutcomeEnum.OK.name())
  //                .httpMethod(httpMethod)
  //                .httpUri(httpUri)
  //                .httpStatusCode(httpStatusCode)
  //                .executionTimeMs(Long.parseLong(executionTimeMs))
  //                .operationErrorDetail(MDC.get(Constants.MDC_ERROR_DETAIL))
  //                .operationErrorCode(MDC.get(Constants.MDC_ERROR_CODE))
  //                .build();
  //    }
  //
  //    @Deprecated
  //    public static ReEventDto createEventForClientCommunication(HttpRequest request,
  // ClientHttpResponse response, byte[] reqBody, WorkflowStatus status, OutcomeEnum outcome) {
  //        String httpMethod = request.getMethod().toString();
  //        String httpUri = request.getURI().toString();
  //        String requestHeaders = formatClientHeaders(request.getHeaders());
  //
  //        String responseHeaders = null;
  //        String requestPayload = null;
  //        String responsePayload = null;
  //        int httpStatusCode = 0;
  //        try {
  //            String plainRequestPayload = new String(reqBody, StandardCharsets.UTF_8);
  //            if (!plainRequestPayload.isBlank()) {
  //                requestPayload = AppBase64Util.base64Encode(ZipUtil.zip(plainRequestPayload));
  //            }
  //            if (response != null) {
  //                responseHeaders = formatClientHeaders(response.getHeaders());
  //                httpStatusCode = response.getStatusCode().value();
  //                String plainResponsePayload = bodyToString(response.getBody());
  //                if (!plainResponsePayload.isBlank()) {
  //                    responsePayload =
  // AppBase64Util.base64Encode(ZipUtil.zip(plainResponsePayload));
  //                }
  //            }
  //
  //        } catch (IOException e) {
  //            log.error(UNZIP_ERROR, e);
  //        }
  //
  //        String executionTimeMs = MDC.get(Constants.MDC_CLIENT_EXECUTION_TIME);
  //        return createBaseRE()
  //                .status(status.name())
  //                .requestPayload(requestPayload)
  //                .responsePayload(responsePayload)
  //                .requestHeaders(requestHeaders)
  //                .responseHeaders(responseHeaders)
  //                .outcome(outcome.name())
  //                .httpMethod(httpMethod)
  //                .httpUri(httpUri)
  //                .httpStatusCode(httpStatusCode)
  //                .executionTimeMs(Long.parseLong(executionTimeMs))
  //                .build();
  //    }
  //
  //    @Deprecated
  //    public static ReEventDto createEventForCommunicationWithCI(String httpUri, List<Pair<String,
  // String>> headers, String plainRequestPayload, ResponseEntity<String> response, OutcomeEnum
  // outcome) {
  //        String httpMethod = "POST";
  //        String requestHeaders = formatRawHeaders(headers);
  //
  //        String requestPayload = null;
  //        String responseHeaders = null;
  //        String responsePayload = null;
  //        int httpStatusCode = 0;
  //        try {
  //            if (!plainRequestPayload.isBlank()) {
  //                requestPayload = AppBase64Util.base64Encode(ZipUtil.zip(plainRequestPayload));
  //            }
  //            if (response != null) {
  //                responseHeaders = formatClientHeaders(response.getHeaders());
  //                httpStatusCode = response.getStatusCode().value();
  //                String plainResponsePayload = response.getBody();
  //                if (plainResponsePayload != null && !plainResponsePayload.isBlank()) {
  //                    responsePayload =
  // AppBase64Util.base64Encode(ZipUtil.zip(plainResponsePayload));
  //                }
  //            }
  //
  //        } catch (IOException e) {
  //            log.error(UNZIP_ERROR, e);
  //        }
  //
  //        String executionTimeMs = MDC.get(Constants.MDC_CLIENT_EXECUTION_TIME);
  //        return createBaseRE()
  //                .status(WorkflowStatus.COMMUNICATION_WITH_CREDITOR_INSTITUTION_PROCESSED.name())
  //                .requestPayload(requestPayload)
  //                .responsePayload(responsePayload)
  //                .requestHeaders(requestHeaders)
  //                .responseHeaders(responseHeaders)
  //                .outcome(outcome.name())
  //                .httpMethod(httpMethod)
  //                .httpUri(httpUri)
  //                .httpStatusCode(httpStatusCode)
  //                .executionTimeMs(Long.parseLong(executionTimeMs))
  //                .build();
  //    }
  //

  //    private static ReEventDto.ReEventDtoBuilder createBaseRE() {
  //        Instant mdcStartTime = MDC.get(Constants.MDC_START_TIME) == null ? null :
  // Instant.ofEpochMilli(Long.parseLong(MDC.get(Constants.MDC_START_TIME)));
  //        return ReEventDto.builder()
  //                .id(UUID.randomUUID().toString())
  //                .operationId(MDC.get(Constants.MDC_OPERATION_ID))
  //                .insertedTimestamp(mdcStartTime)
  //                .businessProcess(MDC.get(Constants.MDC_BUSINESS_PROCESS))
  //                .sessionId(MDC.get(Constants.MDC_SESSION_ID))
  //                .cartId(MDC.get(Constants.MDC_CART_ID))
  //                .iuv(MDC.get(Constants.MDC_IUV))
  //                .noticeNumber(MDC.get(Constants.MDC_NOTICE_NUMBER))
  //                .ccp(MDC.get(Constants.MDC_CCP))
  //                .paymentToken(MDC.get(Constants.MDC_PAYMENT_TOKEN))
  //                .domainId(MDC.get(Constants.MDC_DOMAIN_ID))
  //                .psp(MDC.get(Constants.MDC_PSP_ID))
  //                .station(MDC.get(Constants.MDC_STATION_ID))
  //                .channel(MDC.get(Constants.MDC_CHANNEL_ID));
  //    }

  private static String formatClientHeaders(HttpHeaders headers) {
    try {
      headers.forEach(
          (s, h) -> {
            headers.add(s, StringUtils.join(h, ","));
          });
    } catch (UnsupportedOperationException e) {
      log.debug("Impossible to add formatted header to existing Headers object:", e);
    }

    return formatHeaders(headers);
  }

  private static String formatRawHeaders(List<Pair<String, String>> headersList) {
    HttpHeaders headers = new HttpHeaders();

    try {
      // Group headers by key and join values with commas
      Map<String, String> formattedHeaders =
          headersList.stream()
              .collect(
                  Collectors.groupingBy(
                      Pair::getFirst,
                      Collectors.mapping(Pair::getSecond, Collectors.joining(","))));

      // Add each grouped header to HttpHeaders object
      formattedHeaders.forEach(headers::add);

    } catch (UnsupportedOperationException e) {
      log.debug("Impossible to add formatted header to existing Headers object:", e);
    }

    return formatHeaders(headers);
  }



  private static String formatServerResponseHeaders(HttpServletResponse response) {
    HttpHeaders headers = new HttpHeaders();
    for (String headerName : response.getHeaderNames()) {
      headers.addAll(headerName, response.getHeaders(headerName).stream().toList());
    }

    return formatHeaders(headers);
  }

  private static String formatHeaders(HttpHeaders headers) {
    Stream<String> stream =
        headers.entrySet().stream()
            .map(
                (entry) -> {
                  String values =
                      entry.getValue().stream().collect(Collectors.joining("\", \"", "\"", "\""));
                  return entry.getKey() + ": [" + values + "]";
                });
    return stream.collect(Collectors.joining(", "));
  }

  public static String getRequestMessagePayload(@Nullable HttpServletRequest request) {

    if (request != null) {
      RepeatableContentCachingRequestWrapper wrapper =
          WebUtils.getNativeRequest(request, RepeatableContentCachingRequestWrapper.class);
      try {
        byte[] buf = StreamUtils.copyToByteArray(wrapper.getInputStream());
        if (buf.length > 0) {
          return safelyEncodePayload(buf, wrapper);
        } else {
          return null;
        }
      } catch (IOException e) {
        log.error("Error 'unknown-read'", e);
        return "[unknown-read]";
      }
    }
    return null;
  }

  private static String safelyEncodePayload(
      byte[] buf, RepeatableContentCachingRequestWrapper wrapper) {
    try {
      return new String(buf, wrapper.getCharacterEncoding());
    } catch (UnsupportedEncodingException e) {
      log.error("Error 'unknown-encoding'", e);
      return "[unknown-encoding]";
    }
  }

  private static String getResponseMessagePayload(HttpServletResponse response) {
    ContentCachingResponseWrapper wrapper =
        WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
    if (wrapper != null) {
      byte[] buf = wrapper.getContentAsByteArray();
      if (buf.length > 0) {
        try {
          return new String(buf, wrapper.getCharacterEncoding());
        } catch (UnsupportedEncodingException ex) {
          log.error("Error 'unknown'", ex);
          return "[unknown]";
        }
      }
    }
    return null;
  }

  private static String bodyToString(InputStream body) throws IOException {
    StringBuilder builder = new StringBuilder();
    BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8));
    String line = bufferedReader.readLine();
    while (line != null) {
      builder.append(line).append(System.lineSeparator());
      line = bufferedReader.readLine();
    }
    bufferedReader.close();
    return builder.toString();
  }
}
