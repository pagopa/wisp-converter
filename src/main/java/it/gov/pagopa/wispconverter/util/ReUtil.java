package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.repository.model.enumz.*;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.filter.RepeatableContentCachingRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ReUtil {

    private static final String UNZIP_ERROR = "Unzip error";


    public static ReEventDto.ReEventDtoBuilder getREBuilder() {
        return createBaseRE()
                .eventCategory(EventCategoryEnum.INTERNAL)
                .eventSubcategory(EventSubcategoryEnum.INTERN);
    }


    public static ReEventDto createREForServerInterfaceInRequestEvent(HttpServletRequest request) {
        String httpMethod = request.getMethod();

        StringBuilder msg = new StringBuilder(request.getRequestURI());
        String queryString = request.getQueryString();
        if (queryString != null) {
            msg.append('?').append(queryString);
        }
        String httpUri = msg.toString();
        String httpHeaders = formatServerRequestHeaders(request);
        String httpCallRemoteAddress = request.getRemoteAddr();

        String compressedPayload = null;
        Integer compressedPayloadLength = null;
        try {
            String payload = getRequestMessagePayload(request);
            if (payload != null) {
                compressedPayload = AppBase64Util.base64Encode(ZipUtil.zip(payload));
                compressedPayloadLength = compressedPayload.length();
            }
        } catch (IOException e) {
            log.error(UNZIP_ERROR, e);
        }

        ReEventDto.ReEventDtoBuilder builder = createBaseREForInterfaceEvent(EventSubcategoryEnum.REQ, CallTypeEnum.SERVER, compressedPayload, compressedPayloadLength);
        builder.outcome(OutcomeEnum.RECEIVED)
                .httpMethod(httpMethod)
                .httpUri(httpUri)
                .httpHeaders(httpHeaders)
                .httpCallRemoteAddress(httpCallRemoteAddress);
        return builder.build();
    }

    public static ReEventDto createREForServerInterfaceInResponseEvent(HttpServletRequest request, HttpServletResponse response) {

        String httpHeaders = formatServerResponseHeaders(response);
        String compressedPayload = null;
        Integer compressedPayloadLength = null;
        try {
            String payload = getResponseMessagePayload(response);
            if (payload != null) {
                compressedPayload = AppBase64Util.base64Encode(ZipUtil.zip(payload));
                compressedPayloadLength = compressedPayload.length();
            }
        } catch (IOException e) {
            log.error(UNZIP_ERROR, e);
        }

        int status = response.getStatus();
        String executionTime = MDC.get(Constants.MDC_EXECUTION_TIME);

        String httpMethod = request.getMethod();

        StringBuilder msg = new StringBuilder(request.getRequestURI());
        String queryString = request.getQueryString();
        if (queryString != null) {
            msg.append('?').append(queryString);
        }
        String httpUri = msg.toString();


        ReEventDto.ReEventDtoBuilder builder = createBaseREForInterfaceEvent(EventSubcategoryEnum.RESP, CallTypeEnum.SERVER, compressedPayload, compressedPayloadLength);
        builder.outcome(OutcomeEnum.SEND)
                .httpMethod(httpMethod)
                .httpUri(httpUri)
                .httpHeaders(httpHeaders)
                .httpStatusCode(status)
                .executionTimeMs(Long.parseLong(executionTime))
                .operationStatus(MDC.get(Constants.MDC_STATUS))
                .operationErrorTitle(MDC.get(Constants.MDC_ERROR_TITLE))
                .operationErrorDetail(MDC.get(Constants.MDC_ERROR_DETAIL))
                .operationErrorCode(MDC.get(Constants.MDC_ERROR_CODE));
        return builder.build();
    }

    public static ReEventDto createREForClientInterfaceInRequestEvent(HttpRequest request, byte[] reqBody, ClientEnum clientType, OutcomeEnum outcome) {
        String httpMethod = request.getMethod().toString();
        String httpUri = request.getURI().toString();
        String httpHeaders = formatClientHeaders(request.getHeaders());

        String compressedPayload = null;
        Integer compressedPayloadPayloadLength = null;
        try {
            String payload = new String(reqBody, StandardCharsets.UTF_8);
            if (!payload.isBlank()) {
                compressedPayload = AppBase64Util.base64Encode(ZipUtil.zip(payload));
                compressedPayloadPayloadLength = compressedPayload.length();
            }
        } catch (IOException e) {
            log.error(UNZIP_ERROR, e);
        }

        ReEventDto.ReEventDtoBuilder builder = createBaseREForInterfaceEvent(EventSubcategoryEnum.REQ, CallTypeEnum.CLIENT, compressedPayload, compressedPayloadPayloadLength);
        builder.outcome(outcome)
                .httpMethod(httpMethod)
                .httpUri(httpUri)
                .httpHeaders(httpHeaders)
                .status(InternalStepStatus.getStatusFromClientCommunication(clientType, EventSubcategoryEnum.REQ));

        return builder.build();
    }

    public static ReEventDto createREForClientInterfaceInRequestEvent(String httpMethod, String uri, String headers, String reqBody, ClientEnum clientType, OutcomeEnum outcome) {

        String compressedPayload = null;
        Integer compressedPayloadPayloadLength = null;
        try {
            if (!reqBody.isBlank()) {
                compressedPayload = AppBase64Util.base64Encode(ZipUtil.zip(reqBody));
                compressedPayloadPayloadLength = compressedPayload.length();
            }
        } catch (IOException e) {
            log.error(UNZIP_ERROR, e);
        }

        ReEventDto.ReEventDtoBuilder builder = createBaseREForInterfaceEvent(EventSubcategoryEnum.REQ, CallTypeEnum.CLIENT, compressedPayload, compressedPayloadPayloadLength);
        builder.outcome(outcome)
                .httpMethod(httpMethod)
                .httpUri(uri)
                .httpHeaders(headers)
                .status(InternalStepStatus.getStatusFromClientCommunication(clientType, EventSubcategoryEnum.REQ));

        return builder.build();
    }

    public static ReEventDto createREForClientInterfaceInResponseEvent(HttpRequest request, ClientHttpResponse response, ClientEnum clientType, OutcomeEnum outcome) {
        String httpHeaders = null;
        String compressedPayload = null;
        Integer compressedPayloadPayloadLength = null;
        Integer status = null;
        if (response != null) {
            httpHeaders = formatClientHeaders(response.getHeaders());
            try {
                String payload = bodyToString(response.getBody());
                if (!payload.isBlank()) {
                    compressedPayload = AppBase64Util.base64Encode(ZipUtil.zip(payload));
                    compressedPayloadPayloadLength = compressedPayload.length();
                }
            } catch (IOException e) {
                log.error(UNZIP_ERROR, e);
            }
            try {
                status = response.getStatusCode().value();
            } catch (IOException e) {
                log.error("Retrieve status code error", e);
            }
        }

        String executionTime = MDC.get(Constants.MDC_CLIENT_EXECUTION_TIME);

        String httpMethod = request.getMethod().toString();
        String httpUri = request.getURI().toString();

        ReEventDto.ReEventDtoBuilder builder = createBaseREForInterfaceEvent(EventSubcategoryEnum.RESP, CallTypeEnum.CLIENT, compressedPayload, compressedPayloadPayloadLength);
        builder.outcome(outcome)
                .httpMethod(httpMethod)
                .httpUri(httpUri)
                .httpHeaders(httpHeaders)
                .httpStatusCode(status)
                .executionTimeMs(Long.parseLong(executionTime))
                .status(InternalStepStatus.getStatusFromClientCommunication(clientType, EventSubcategoryEnum.RESP));
        return builder.build();
    }

    public static ReEventDto createREForClientInterfaceInResponseEvent(String httpMethod, String uri, HttpHeaders headers, int status, String resBody, ClientEnum clientType, OutcomeEnum outcome) {

        String compressedPayload = null;
        Integer compressedPayloadPayloadLength = null;

        try {
            String payload = resBody;
            if (!payload.isBlank()) {
                compressedPayload = AppBase64Util.base64Encode(ZipUtil.zip(payload));
                compressedPayloadPayloadLength = compressedPayload.length();
            }
        } catch (IOException e) {
            log.error(UNZIP_ERROR, e);
        }

        String executionTime = MDC.get(Constants.MDC_CLIENT_EXECUTION_TIME);

        ReEventDto.ReEventDtoBuilder builder = createBaseREForInterfaceEvent(EventSubcategoryEnum.RESP, CallTypeEnum.CLIENT, compressedPayload, compressedPayloadPayloadLength);
        builder.outcome(outcome)
                .httpMethod(httpMethod)
                .httpUri(uri)
                .httpHeaders(headers != null ? formatClientHeaders(headers) : null)
                .httpStatusCode(status)
                .executionTimeMs(Long.parseLong(executionTime))
                .status(InternalStepStatus.getStatusFromClientCommunication(clientType, EventSubcategoryEnum.RESP));
        return builder.build();
    }

    private static ReEventDto.ReEventDtoBuilder createBaseREForInterfaceEvent(EventSubcategoryEnum eventSubcategory, CallTypeEnum callTypeEnum,
                                                                              String compressedPayload, Integer compressedPayloadLength) {
        return createBaseRE()
                .eventCategory(EventCategoryEnum.INTERFACE)
                .eventSubcategory(eventSubcategory)
                .callType(callTypeEnum)
                .compressedPayload(compressedPayload)
                .compressedPayloadLength(compressedPayloadLength);
    }

    private static ReEventDto.ReEventDtoBuilder createBaseRE() {
        Instant mdcStartTime = MDC.get(Constants.MDC_START_TIME) == null ? null : Instant.ofEpochMilli(Long.parseLong(MDC.get(Constants.MDC_START_TIME)));
        return ReEventDto.builder()
                .id(UUID.randomUUID().toString())
                .requestId(MDC.get(Constants.MDC_REQUEST_ID))
                .operationId(MDC.get(Constants.MDC_OPERATION_ID))
                .clientOperationId(MDC.get(Constants.MDC_CLIENT_OPERATION_ID))
                .component(ComponentEnum.WISP_CONVERTER)
                .insertedTimestamp(mdcStartTime)
                .businessProcess(MDC.get(Constants.MDC_BUSINESS_PROCESS))
                .sessionId(MDC.get(Constants.MDC_SESSION_ID))
                .primitive(MDC.get(Constants.MDC_PRIMITIVE))
                .cartId(MDC.get(Constants.MDC_CART_ID))
                .iuv(MDC.get(Constants.MDC_IUV))
                .noticeNumber(MDC.get(Constants.MDC_NOTICE_NUMBER))
                .ccp(MDC.get(Constants.MDC_CCP))
                .domainId(MDC.get(Constants.MDC_DOMAIN_ID))
                .psp(MDC.get(Constants.MDC_PSP_ID))
                .station(MDC.get(Constants.MDC_STATION_ID))
                .channel(MDC.get(Constants.MDC_CHANNEL_ID));
    }

    private static String formatClientHeaders(HttpHeaders headers) {
        headers.forEach((s, h) -> {
            headers.add(s, StringUtils.join(h, ","));
        });

        return formatHeaders(headers);
    }

    private static String formatServerRequestHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNamesEnum = request.getHeaderNames();
        while (headerNamesEnum.hasMoreElements()) {
            String headerName = headerNamesEnum.nextElement();
            Iterator<String> iterator = request.getHeaders(headerName).asIterator();
            while (iterator.hasNext()) {
                headers.add(headerName, iterator.next());
            }
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
        Stream<String> stream = headers.entrySet().stream()
                .map((entry) -> {
                    String values = entry.getValue().stream().collect(Collectors.joining("\", \"", "\"", "\""));
                    return entry.getKey() + ": [" + values + "]";
                });
        return stream.collect(Collectors.joining(", "));
    }

    private static String getRequestMessagePayload(HttpServletRequest request) {
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

    private static String safelyEncodePayload(byte[] buf, RepeatableContentCachingRequestWrapper wrapper) {
        try {
            return new String(buf, wrapper.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            log.error("Error 'unknown-encoding'", e);
            return "[unknown-encoding]";
        }
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

    private static String bodyToString(InputStream body) throws IOException {
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
