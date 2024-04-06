package it.gov.pagopa.wispconverter.util.filter;

import it.gov.pagopa.wispconverter.util.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Slf4j
public abstract class AbstractAppServerLoggingFilter extends OncePerRequestFilter {

  public static final String REQUEST_DEFAULT_MESSAGE_PREFIX = "=> SERVER Request OPERATION_ID=%s - ";
  public static final String RESPONSE_DEFAULT_MESSAGE_PREFIX = "<= SERVER Response OPERATION_ID=%s - ";
  private static final int REQUEST_DEFAULT_MAX_PAYLOAD_LENGTH = 50;
  private static final int RESPONSE_DEFAULT_MAX_PAYLOAD_LENGTH = 50;

  private static final String SPACE = " ";
  private static final String PRETTY_IN = "\n=> *";
  private static final String PRETTY_OUT = "\n<= *";

  private boolean requestIncludeClientInfo = false;

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

  private List<String> excludeUrlPatterns;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    try {
      String operationId = UUID.randomUUID().toString();
      MDC.put(Constants.MDC_START_TIME, String.valueOf(System.currentTimeMillis()));
      MDC.put(Constants.MDC_OPERATION_ID, operationId);

      request(operationId, request);
      filterChain.doFilter(request, response);
    } finally {
      String operationId = MDC.get(Constants.MDC_OPERATION_ID);
      String executionTime = MDC.get(Constants.MDC_EXECUTION_TIME);
      MDC.put(Constants.MDC_EXECUTION_TIME, executionTime);
      response(operationId, executionTime, request, response);
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    AntPathMatcher pathMatcher = new AntPathMatcher();
    return excludeUrlPatterns
            .stream()
            .anyMatch(p -> pathMatcher.match(p, request.getServletPath()));
  }

  private static String getExecutionTime(String startTime) {
    if (startTime != null) {
      long endTime = System.currentTimeMillis();
      long executionTime = endTime - Long.parseLong(startTime);
      return String.valueOf(executionTime);
    }
    return "-";
  }


  protected String createRequestMessage(String operationId, HttpServletRequest request) {
    StringBuilder msg = new StringBuilder();
    msg.append(String.format(REQUEST_DEFAULT_MESSAGE_PREFIX, operationId));
    if(isRequestPretty()){
      msg.append(PRETTY_IN).append(SPACE);
    }
    msg.append("path: ").append(request.getMethod()).append(' ');
    msg.append(request.getRequestURI());

    String queryString = request.getQueryString();
    if (queryString != null) {
      msg.append('?').append(queryString);
    }

    if (isRequestIncludeClientInfo()) {
      String client = request.getRemoteAddr();
      if (StringUtils.hasLength(client)) {
        if(isRequestPretty()){
          msg.append(PRETTY_IN).append(SPACE);
        } else{
          msg.append(", ");
        }
        msg.append("client: ").append(client);
      }
      HttpSession session = request.getSession(false);
      if (session != null) {
        if(isRequestPretty()){
          msg.append(PRETTY_IN).append(SPACE);
        } else{
          msg.append(", ");
        }
        msg.append(", session: ").append(session.getId());
      }
      String user = request.getRemoteUser();
      if (user != null) {
        if(isRequestPretty()){
          msg.append(PRETTY_IN).append(SPACE);
        } else{
          msg.append(", ");
        }
        msg.append(", user: ").append(user);
      }
    }

    if (isRequestIncludeHeaders()) {
      HttpHeaders headers = new HttpHeaders();
      Enumeration<String> headerNamesEnum = request.getHeaderNames();
      while (headerNamesEnum.hasMoreElements()) {
        String headerName = headerNamesEnum.nextElement();
        if (getRequestHeaderPredicate() != null) {
          if (!getRequestHeaderPredicate().test(headerName)) {
            headers.add(headerName, "masked");
          } else {
            Iterator<String> iterator = request.getHeaders(headerName).asIterator();
            while (iterator.hasNext()) {
              headers.add(headerName, iterator.next());
            }
          }
        } else {
          Iterator<String> iterator = request.getHeaders(headerName).asIterator();
          while (iterator.hasNext()) {
            headers.add(headerName, iterator.next());
          }
      }
    }
      String formatRequestHeaders = formatRequestHeaders(headers);
      if(formatRequestHeaders!=null){
        if(isRequestPretty()){
          msg.append(PRETTY_IN).append(SPACE);
        } else{
          msg.append(", ");
        }
        msg.append("headers: [").append(formatRequestHeaders);
        if(isRequestPretty()){
          msg.append(PRETTY_IN).append(SPACE);
        }
        msg.append("]");
      }
    }

    if (isRequestIncludePayload()) {
      String payload = getRequestMessagePayload(request);
      if (payload != null) {
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

  protected String createResponseMessage(String id, String executionTime, HttpServletRequest request, HttpServletResponse response) {
    StringBuilder msg = new StringBuilder();
    msg.append(String.format(RESPONSE_DEFAULT_MESSAGE_PREFIX, id));

    if(isResponsePretty()){
      msg.append(PRETTY_OUT).append(SPACE);
    }
    msg.append("path: ").append(request.getMethod()).append(' ');
    msg.append(request.getRequestURI());

    String queryString = request.getQueryString();
    if (queryString != null) {
      msg.append('?').append(queryString);
    }

    if(isResponsePretty()){
      msg.append(PRETTY_OUT).append(SPACE);
    }
    msg.append("status: ").append(response.getStatus());
    if(isResponsePretty()){
      msg.append(PRETTY_OUT).append(SPACE);
    } else{
      msg.append(", ");
    }
    msg.append("execution-time: ").append(executionTime).append("ms");

    if (isResponseIncludeHeaders()) {
      HttpHeaders headers = new HttpHeaders();
      for(String headerName : response.getHeaderNames()){
        if (getResponseHeaderPredicate() != null) {
          if (!getResponseHeaderPredicate().test(headerName)) {
            headers.add(headerName, "masked");
          } else {
            headers.addAll(headerName, response.getHeaders(headerName).stream().toList());
          }
        } else {
          headers.addAll(headerName, response.getHeaders(headerName).stream().toList());
        }
      }
      String formatResponseHeaders = formatResponseHeaders(headers);
      if(formatResponseHeaders!=null){
        if(isRequestPretty()){
          msg.append(PRETTY_OUT).append(SPACE);
        } else{
          msg.append(", ");
        }
        msg.append("headers: [").append(formatResponseHeaders);
        if(isRequestPretty()){
          msg.append(PRETTY_IN).append(SPACE);
        }
        msg.append("]");
      }
    }

    if (isResponseIncludePayload()) {
      String payload = getResponseMessagePayload(response);
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


  private String getRequestMessagePayload(HttpServletRequest request) {
    RepeatableContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, RepeatableContentCachingRequestWrapper.class);
    if (wrapper != null) {
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

  private String safelyEncodePayload(byte[] buf, RepeatableContentCachingRequestWrapper wrapper) {
    int length = Math.min(buf.length, getRequestMaxPayloadLength());
    try {
      return new String(buf, 0, length, wrapper.getCharacterEncoding());
    } catch (UnsupportedEncodingException e) {
      log.error("Error 'unknown-encoding'", e);
      return "[unknown-encoding]";
    }
  }


  private String getResponseMessagePayload(HttpServletResponse response) {
    ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
    if (wrapper != null) {
      byte[] buf = wrapper.getContentAsByteArray();
      if (buf.length > 0) {
        int length = Math.min(buf.length, getResponseMaxPayloadLength());
        try {
          return new String(buf, 0, length, wrapper.getCharacterEncoding());
        } catch (UnsupportedEncodingException ex) {
          log.error("Error 'unknown'", ex);
          return "[unknown]";
        }
      }
    }
    return null;
  }

  protected abstract void request(String operationId, HttpServletRequest request);

  protected abstract void response(String operationId, String executionTime, HttpServletRequest request, HttpServletResponse response);

  private String formatRequestHeaders(MultiValueMap<String, String> headers) {
    Stream<String> stream = headers.entrySet().stream()
            .map((entry) -> {
              if(isRequestPretty()){
                String values = entry.getValue().stream().collect(Collectors.joining("\", \"","\"","\""));
                return PRETTY_IN +"*\t"+entry.getKey() + ": [" + values + "]";
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
                return PRETTY_OUT +"*\t"+entry.getKey().toLowerCase() + ": [" + values + "]";
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
}
