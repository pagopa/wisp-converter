package it.gov.pagopa.wispconverter.util.interceptor;

import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.Trace;
import it.gov.pagopa.wispconverter.util.client.RequestResponseLoggingProperties;
import it.gov.pagopa.wispconverter.util.filter.RepeatableContentCachingRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractAppServerLoggingInterceptor implements HandlerInterceptor {

    public static final String REQUEST_DEFAULT_MESSAGE_PREFIX = "=> SERVER Request OPERATION_ID=%s - ";
    public static final String RESPONSE_DEFAULT_MESSAGE_PREFIX = "<= SERVER Response OPERATION_ID=%s - ";
    private static final int REQUEST_DEFAULT_MAX_PAYLOAD_LENGTH = 50;
    private static final int RESPONSE_DEFAULT_MAX_PAYLOAD_LENGTH = 50;
    private static final Collector<CharSequence, ?, String> JOINCOLLECTOR = Collectors.joining("\", \"", "\"", "\"");
    private static final String SPACE = " ";
    private static final String PRETTY_IN = "\n=> *";
    private static final String PRETTY_OUT = "\n<= *";
    private final Pattern sessionIdPattern;
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
    @Setter
    private List<String> excludeUrlPatterns;

    public AbstractAppServerLoggingInterceptor(RequestResponseLoggingProperties serverLoggingProperties) {
        if (serverLoggingProperties != null) {
            RequestResponseLoggingProperties.Request request = serverLoggingProperties.getRequest();
            if (request != null) {
                this.requestIncludeHeaders = request.isIncludeHeaders();
                this.requestIncludePayload = request.isIncludePayload();
                this.requestMaxPayloadLength = request.getMaxPayloadLength() != null ? request.getMaxPayloadLength() : REQUEST_DEFAULT_MAX_PAYLOAD_LENGTH;
                this.requestHeaderPredicate = s -> !s.equals(request.getMaskHeaderName());
                this.requestPretty = request.isPretty();
                this.requestIncludeClientInfo = request.isIncludeClientInfo();
            }
            RequestResponseLoggingProperties.Response response = serverLoggingProperties.getResponse();
            if (response != null) {
                this.responseIncludeHeaders = response.isIncludeHeaders();
                this.responseIncludePayload = response.isIncludePayload();
                this.responseMaxPayloadLength = response.getMaxPayloadLength() != null ? response.getMaxPayloadLength() : RESPONSE_DEFAULT_MAX_PAYLOAD_LENGTH;
                this.responseHeaderPredicate = null;
                this.responsePretty = response.isPretty();
            }
        }
        this.sessionIdPattern = Pattern.compile("sessionId=([a-zA-Z0-9_-]+)");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            Trace trace = handlerMethod.getMethod().getAnnotation(Trace.class);
            if (trace != null) {
                handleMDCSessionContent(request, trace);
                request(MDC.get(Constants.MDC_OPERATION_ID), request);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            Trace trace = handlerMethod.getMethod().getAnnotation(Trace.class);
            if (trace != null) {
                String operationId = MDC.get(Constants.MDC_OPERATION_ID);
                String executionTime = MDC.get(Constants.MDC_EXECUTION_TIME);
                MDC.put(Constants.MDC_EXECUTION_TIME, executionTime);
                response(operationId, executionTime, request, response);
            }
        }
    }


    protected String createRequestMessage(String operationId, HttpServletRequest request) {
        StringBuilder msg = new StringBuilder();
        msg.append(String.format(REQUEST_DEFAULT_MESSAGE_PREFIX, operationId));
        if (this.requestPretty) {
            msg.append(PRETTY_IN).append(SPACE);
        }
        msg.append("path: ").append(request.getMethod()).append(' ');
        msg.append(request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null) {
            msg.append('?').append(queryString);
        }

        if (this.requestIncludeClientInfo) {
            String client = request.getRemoteAddr();
            if (StringUtils.hasLength(client)) {
                if (this.requestPretty) {
                    msg.append(PRETTY_IN).append(SPACE);
                } else {
                    msg.append(", ");
                }
                msg.append("client: ").append(client);
            }
            HttpSession session = request.getSession(false);
            if (session != null) {
                if (this.requestPretty) {
                    msg.append(PRETTY_IN).append(SPACE);
                } else {
                    msg.append(", ");
                }
                msg.append(", session: ").append(session.getId());
            }
            String user = request.getRemoteUser();
            if (user != null) {
                if (this.requestPretty) {
                    msg.append(PRETTY_IN).append(SPACE);
                } else {
                    msg.append(", ");
                }
                msg.append(", user: ").append(user);
            }
        }

        if (this.requestIncludeHeaders) {
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNamesEnum = request.getHeaderNames();
            while (headerNamesEnum.hasMoreElements()) {
                String headerName = headerNamesEnum.nextElement();
                if (this.requestHeaderPredicate != null) {
                    if (!this.requestHeaderPredicate.test(headerName)) {
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
            if (formatRequestHeaders != null) {
                if (this.requestPretty) {
                    msg.append(PRETTY_IN).append(SPACE);
                } else {
                    msg.append(", ");
                }
                msg.append("headers: [").append(formatRequestHeaders);
                if (this.requestPretty) {
                    msg.append(PRETTY_IN).append(SPACE);
                }
                msg.append("]");
            }
        }

        if (this.requestIncludePayload) {
            String payload = getRequestMessagePayload(request);
            if (payload != null) {
                if (this.requestPretty) {
                    msg.append(PRETTY_IN).append(SPACE);
                } else {
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

        if (this.responsePretty) {
            msg.append(PRETTY_OUT).append(SPACE);
        }
        msg.append("path: ").append(request.getMethod()).append(' ');
        msg.append(request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null) {
            msg.append('?').append(queryString);
        }

        if (this.responsePretty) {
            msg.append(PRETTY_OUT).append(SPACE);
        }
        msg.append("status: ").append(response.getStatus());
        if (this.responsePretty) {
            msg.append(PRETTY_OUT).append(SPACE);
        } else {
            msg.append(", ");
        }
        msg.append("execution-time: ").append(executionTime).append("ms");

        if (this.responseIncludeHeaders) {
            HttpHeaders headers = new HttpHeaders();
            for (String headerName : response.getHeaderNames()) {
                if (this.responseHeaderPredicate != null) {
                    if (!this.responseHeaderPredicate.test(headerName)) {
                        headers.add(headerName, "masked");
                    } else {
                        headers.addAll(headerName, response.getHeaders(headerName).stream().toList());
                    }
                } else {
                    headers.addAll(headerName, response.getHeaders(headerName).stream().toList());
                }
            }
            String formatResponseHeaders = formatResponseHeaders(headers);
            if (formatResponseHeaders != null) {
                if (this.requestPretty) {
                    msg.append(PRETTY_OUT).append(SPACE);
                } else {
                    msg.append(", ");
                }
                msg.append("headers: [").append(formatResponseHeaders);
                if (this.requestPretty) {
                    msg.append(PRETTY_IN).append(SPACE);
                }
                msg.append("]");
            }
        }

        if (this.responseIncludePayload) {
            String payload = getResponseMessagePayload(response);
            if (payload != null) {
                if (this.requestPretty) {
                    msg.append(PRETTY_OUT).append(SPACE);
                } else {
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
        int length = Math.min(buf.length, this.requestMaxPayloadLength);
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
                int length = Math.min(buf.length, this.responseMaxPayloadLength);
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
                    if (this.requestPretty) {
                        String values = entry.getValue().stream().collect(JOINCOLLECTOR);
                        return PRETTY_IN + "*\t" + entry.getKey() + ": [" + values + "]";
                    } else {
                        String values = entry.getValue().stream().collect(JOINCOLLECTOR);
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
        Stream<String> stream = headers.entrySet().stream()
                .map((entry) -> {
                    if (this.responsePretty) {
                        String values = entry.getValue().stream().collect(JOINCOLLECTOR);
                        return PRETTY_OUT + "*\t" + entry.getKey().toLowerCase() + ": [" + values + "]";
                    } else {
                        String values = entry.getValue().stream().collect(JOINCOLLECTOR);
                        return entry.getKey().toLowerCase() + ": [" + values + "]";
                    }
                });
        if (this.requestPretty) {
            return stream.collect(Collectors.joining(""));
        } else {
            return stream.collect(Collectors.joining(", "));
        }
    }

    private void handleMDCSessionContent(HttpServletRequest request, Trace trace) {
        String operationId = UUID.randomUUID().toString();
        MDC.put(Constants.MDC_START_TIME, String.valueOf(System.currentTimeMillis()));
        MDC.put(Constants.MDC_OPERATION_ID, operationId);
        MDC.put(Constants.MDC_BUSINESS_PROCESS, trace.businessProcess());

        String queryString = request.getQueryString();

        if(queryString != null) {
            // include sessionID in MDC
            Matcher sessionIdMatcher = this.sessionIdPattern.matcher(queryString);
            if (sessionIdMatcher.find()) {
                MDC.put(Constants.MDC_SESSION_ID, sessionIdMatcher.group(1));
            }
        }
    }
}
