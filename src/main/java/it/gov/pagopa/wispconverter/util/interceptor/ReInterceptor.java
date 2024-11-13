package it.gov.pagopa.wispconverter.util.interceptor;

import it.gov.pagopa.wispconverter.repository.model.enumz.OutcomeEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.service.model.re.ReRequestContext;
import it.gov.pagopa.wispconverter.service.model.re.ReResponseContext;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.EndpointRETrace;
import it.gov.pagopa.wispconverter.util.filter.RepeatableContentCachingRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import static it.gov.pagopa.wispconverter.util.Constants.MDC_STATUS;

@Slf4j
@RequiredArgsConstructor
public class ReInterceptor implements HandlerInterceptor {

    private final ReService reService;

    private static OutcomeEnum getOutcomeEnum(HttpServletResponse response) {
        String outcomeName = MDC.get(Constants.MDC_OUTCOME);
        OutcomeEnum outcome;
        if (outcomeName == null) {
            outcome = response.getStatus() > 399 ? OutcomeEnum.ERROR : OutcomeEnum.OK;
        } else {
            outcome = OutcomeEnum.valueOf(outcomeName);
        }
        return outcome;
    }

    private static HttpHeaders formatServerRequestHeaders(HttpServletResponse request) {
        HttpHeaders headers = new HttpHeaders();
        var headerNamesEnum = request.getHeaderNames();
        for (var headerName : headerNamesEnum) {
            List<String> iterator = request.getHeaders(headerName).stream().toList();
            headers.addAll(headerName, iterator);
        }
        return headers;
    }

    private static HttpHeaders formatServerRequestHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNamesEnum = request.getHeaderNames();
        while (headerNamesEnum.hasMoreElements()) {
            String headerName = headerNamesEnum.nextElement();
            Iterator<String> iterator = request.getHeaders(headerName).asIterator();
            while (iterator.hasNext()) {
                headers.add(headerName, iterator.next());
            }
        }

        return headers;
    }

    private static String getResponseMessagePayload(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
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

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (handler instanceof HandlerMethod handlerMethod) {
            EndpointRETrace trace = handlerMethod.getMethod().getAnnotation(EndpointRETrace.class);
            if (trace != null && trace.reEnabled()) {
                String businessProcess = trace.businessProcess();
                log.debug("[afterCompletion] trace RE SERVER OUT businessProcess = [{}]", businessProcess);
                MDC.put(MDC_STATUS, trace.status().name());
                MDC.put(Constants.MDC_CLIENT_EXECUTION_TIME, CommonUtility.getExecutionTime(CommonUtility.getExecutionTime(MDC.get(Constants.MDC_START_TIME))));

                String requestUri = request.getRequestURI();
                String queryString = request.getQueryString();
                if (queryString != null && !queryString.isEmpty()) {
                    requestUri += "?" + queryString;
                }

                reService.sendEvent(
                        WorkflowStatus.valueOf(MDC.get(MDC_STATUS)),
                        MDC.get(Constants.MDC_INFO),
                        getOutcomeEnum(response),
                        ReRequestContext.builder()
                                .uri(requestUri)
                                .method(HttpMethod.valueOf(request.getMethod()))
                                .payload(getRequestMessagePayload(request))
                                .headers(formatServerRequestHeaders(request))
                                .build(),
                        ReResponseContext.builder()
                                .statusCode(HttpStatus.valueOf(response.getStatus()))
                                .payload(getResponseMessagePayload(response))
                                .headers(formatServerRequestHeaders(response))
                                .build());
            }
        }
    }
}
