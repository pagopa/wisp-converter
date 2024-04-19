package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.service.model.re.*;
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

    private static final String NODO_DEI_PAGAMENTI_SP = "NodoDeiPagamentiSPC";
    private static final String UNZIP_ERROR = "Unzip error";

    private static ReEventDto.ReEventDtoBuilder createBaseReInterface(CategoriaEventoEnum categoriaEvento, SottoTipoEventoEnum sottoTipoEvento, EsitoEnum esitoEnum, String erogatore, String erogatoreDescr, String fruitore, String fruitoreDescr,
                                                                      String httpMethod, String httpUri, String httpHeaders, String httpCallRemoteAddress, String compressedPayload, Integer compressedPayloadLength,
                                                                      CallTypeEnum callTypeEnum) {

        return createBaseReBuilder()
                .categoriaEvento(categoriaEvento)
                .sottoTipoEvento(sottoTipoEvento)
                .callType(callTypeEnum)
                .fruitore(fruitore)
                .fruitoreDescr(fruitoreDescr)
                .erogatore(erogatore)
                .erogatoreDescr(erogatoreDescr)
                .esito(esitoEnum)
                .httpMethod(httpMethod)
                .httpUri(httpUri)
                .httpHeaders(httpHeaders)
                .httpCallRemoteAddress(httpCallRemoteAddress)
                .compressedPayload(compressedPayload)
                .compressedPayloadLength(compressedPayloadLength);
    }

    private static ReEventDto.ReEventDtoBuilder createBaseReBuilder() {
        Instant mdcStartTime = MDC.get(Constants.MDC_START_TIME) == null ? null : Instant.ofEpochMilli(Long.parseLong(MDC.get(Constants.MDC_START_TIME)));
        return ReEventDto.builder()
                .id(UUID.randomUUID().toString())
                .requestId(MDC.get(Constants.MDC_REQUEST_ID))
                .operationId(MDC.get(Constants.MDC_OPERATION_ID))
                .clientOperationId(MDC.get(Constants.MDC_CLIENT_OPERATION_ID))
                .componente(ComponenteEnum.WISP_CONVERTER)
                .insertedTimestamp(mdcStartTime)
                .businessProcess(MDC.get(Constants.MDC_BUSINESS_PROCESS));
    }

    public static ReEventDto.ReEventDtoBuilder createBaseReInternal() {
        return createBaseReBuilder()
                .categoriaEvento(CategoriaEventoEnum.INTERNO)
                .sottoTipoEvento(SottoTipoEventoEnum.INTERN);
    }


    public static ReEventDto createReServerInterfaceRequest(HttpServletRequest request) {
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


        return createBaseReInterface(
                CategoriaEventoEnum.INTERFACCIA,
                SottoTipoEventoEnum.REQ,
                EsitoEnum.RICEVUTA,
                NODO_DEI_PAGAMENTI_SP, NODO_DEI_PAGAMENTI_SP,
                null, null,
                httpMethod, httpUri, httpHeaders, httpCallRemoteAddress, compressedPayload, compressedPayloadLength,
                CallTypeEnum.SERVER)
                .build();
    }

    public static ReEventDto createReServerInterfaceResponse(HttpServletRequest request, HttpServletResponse response) {

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

        ReEventDto.ReEventDtoBuilder target = createBaseReInterface(
                CategoriaEventoEnum.INTERFACCIA,
                SottoTipoEventoEnum.RESP,
                EsitoEnum.INVIATA,
                NODO_DEI_PAGAMENTI_SP, NODO_DEI_PAGAMENTI_SP,
                null, null,
                httpMethod, httpUri, httpHeaders, null, compressedPayload, compressedPayloadLength,
                CallTypeEnum.SERVER);

        target.httpStatusCode(status);
        target.executionTimeMs(Long.parseLong(executionTime));

        target.operationStatus(MDC.get(Constants.MDC_STATUS));
        target.operationErrorTitle(MDC.get(Constants.MDC_ERROR_TITLE));
        target.operationErrorDetail(MDC.get(Constants.MDC_ERROR_DETAIL));
        target.operationErrorCode(MDC.get(Constants.MDC_ERROR_CODE));
        return target.build();
    }

    public static ReEventDto createReClientInterfaceRequest(HttpRequest request, byte[] reqBody, EsitoEnum esitoEnum) {
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

        String erogatore = MDC.get(Constants.MDC_EROGATORE);
        String erogatoreDescr = MDC.get(Constants.MDC_EROGATORE_DESCR);

        return createBaseReInterface(
                CategoriaEventoEnum.INTERFACCIA,
                SottoTipoEventoEnum.REQ,
                esitoEnum,
                erogatore, erogatoreDescr,
                NODO_DEI_PAGAMENTI_SP, NODO_DEI_PAGAMENTI_SP,
                httpMethod, httpUri, httpHeaders, null, compressedPayload, compressedPayloadPayloadLength,
                CallTypeEnum.CLIENT)
                .build();
    }

    public static ReEventDto createReClientInterfaceResponse(HttpRequest request, ClientHttpResponse response, EsitoEnum esitoEnum) {
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

        String erogatore = MDC.get(Constants.MDC_EROGATORE);
        String erogatoreDescr = MDC.get(Constants.MDC_EROGATORE_DESCR);

        String httpMethod = request.getMethod().toString();
        String httpUri = request.getURI().toString();

        ReEventDto.ReEventDtoBuilder target = createBaseReInterface(
                CategoriaEventoEnum.INTERFACCIA,
                SottoTipoEventoEnum.RESP,
                esitoEnum,
                erogatore, erogatoreDescr,
                NODO_DEI_PAGAMENTI_SP, NODO_DEI_PAGAMENTI_SP,
                httpMethod, httpUri, httpHeaders, null, compressedPayload, compressedPayloadPayloadLength,
                CallTypeEnum.CLIENT);

        target.httpStatusCode(status);
        target.executionTimeMs(Long.parseLong(executionTime));

        return target.build();
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
