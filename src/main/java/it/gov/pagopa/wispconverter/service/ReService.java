package it.gov.pagopa.wispconverter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.service.mapper.ReMapper;
import it.gov.pagopa.wispconverter.service.model.re.*;
import it.gov.pagopa.wispconverter.util.AppBase64Util;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ZipUtil;
import it.gov.pagopa.wispconverter.util.filter.RepeatableContentCachingRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReService {

    private final ObjectMapper objectMapper;


    public void addRe(ReEventDto reEventDto) {
        try {
            log.info("\n#################\n# RE INTERFACE \n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reEventDto) + "\n#################");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String NODO_DEI_PAGAMENTI_SP = "NodoDeiPagamentiSPC";

    private static ReEventDto createReFromMDC(CategoriaEventoEnum categoriaEvento, SottoTipoEventoEnum sottoTipoEvento, EsitoEnum esitoEnum, String erogatore, String erogatoreDescr, String fruitore, String fruitoreDescr,
                                             String httpMethod, String httpUri, String httpHeaders, String httpCallRemoteAddress, String compressedPayload, Integer compressedPayloadPayloadLength) {

        return ReEventDto.builder()
                .requestId(MDC.get(Constants.MDC_REQUEST_ID))
                .operationId(MDC.get(Constants.MDC_OPERATION_ID))
                .clientOperationId(MDC.get(Constants.MDC_CLIENT_OPERATION_ID))
                .insertedTimestamp(Instant.ofEpochMilli(Long.parseLong(MDC.get(Constants.MDC_START_TIME))))
                .componente(ComponenteEnum.WISP_CONVERTER)
                .categoriaEvento(categoriaEvento)
                .sottoTipoEvento(sottoTipoEvento)
                .esito(esitoEnum)
                .compressedPayload(compressedPayload)
                .compressedPayloadPayloadLength(compressedPayloadPayloadLength)
                .erogatore(erogatore)
                .erogatoreDescr(erogatoreDescr)
                .fruitore(fruitore)
                .fruitoreDescr(fruitoreDescr)
                .httpMethod(httpMethod)
                .httpUri(httpUri)
                .httpHeaders(httpHeaders)
                .httpCallRemoteAddress(httpCallRemoteAddress)
                .build();
    }

    public static ReEventDto createReServerInterfaceRequest(HttpServletRequest request){
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
        Integer compressedPayloadPayloadLength = null;
        try {
            String payload = getRequestMessagePayload(request);
            if(payload!=null){
                compressedPayload = AppBase64Util.base64Encode(ZipUtil.zip(payload));
                compressedPayloadPayloadLength = compressedPayload.length();
            }
        } catch (IOException e) {
            log.error("Unzip error", e);
        }


        return createReFromMDC(CategoriaEventoEnum.INTERFACCIA, SottoTipoEventoEnum.REQ, EsitoEnum.RICEVUTA, NODO_DEI_PAGAMENTI_SP, NODO_DEI_PAGAMENTI_SP, null, null,
                httpMethod, httpUri, httpHeaders, httpCallRemoteAddress, compressedPayload, compressedPayloadPayloadLength);
    }
    public static ReEventDto createReServerInterfaceResponse(HttpServletResponse response, ReEventDto source){

        String httpHeaders = formatServerResponseHeaders(response);
        String compressedPayload = null;
        Integer compressedPayloadPayloadLength = null;
        try {
            String payload = getResponseMessagePayload(response);
            if(payload!=null) {
                compressedPayload = AppBase64Util.base64Encode(ZipUtil.zip(payload));
                compressedPayloadPayloadLength = compressedPayload.length();
            }
        } catch (IOException e) {
            log.error("Unzip error", e);
        }

        int status = response.getStatus();
        String executionTime = MDC.get(Constants.MDC_EXECUTION_TIME);

        ReEventDto target = ReMapper.INSTANCE.clone(source);
        target.setSottoTipoEvento(SottoTipoEventoEnum.RESP);
        target.setEsito(EsitoEnum.INVIATA);
        target.setHttpHeaders(httpHeaders);
        target.setHttpCallRemoteAddress(null);
        target.setCompressedPayload(compressedPayload);
        target.setCompressedPayloadPayloadLength(compressedPayloadPayloadLength);
        target.setHttpStatusCode(status);
        target.setExecutionTimeMs(executionTime);
        return target;
    }

    public static ReEventDto createReClientInterfaceRequest(HttpRequest request, byte[] reqBody){
        String httpMethod = request.getMethod().toString();
        String httpUri = request.getURI().toString();
        String httpHeaders = formatClientHeaders(request.getHeaders());

        String compressedPayload = null;
        Integer compressedPayloadPayloadLength = null;
        try {
            String payload = new String(reqBody, StandardCharsets.UTF_8);
            if(!payload.isBlank()) {
                compressedPayload = AppBase64Util.base64Encode(ZipUtil.zip(payload));
                compressedPayloadPayloadLength = compressedPayload.length();
            }
        } catch (IOException e) {
            log.error("Unzip error", e);
        }

        String erogatore = MDC.get(Constants.MDC_EROGATORE);
        String erogatoreDescr = MDC.get(Constants.MDC_EROGATORE_DESCR);

        //FIXME INVIATA o INVIATA_KO
        return createReFromMDC(CategoriaEventoEnum.INTERFACCIA, SottoTipoEventoEnum.REQ, EsitoEnum.INVIATA, erogatore, erogatoreDescr, NODO_DEI_PAGAMENTI_SP, NODO_DEI_PAGAMENTI_SP,
                httpMethod, httpUri, httpHeaders, null, compressedPayload, compressedPayloadPayloadLength);
    }

    public static ReEventDto createReClientInterfaceResponse(ClientHttpResponse response, ReEventDto source){

        String httpHeaders = formatClientHeaders(response.getHeaders());
        String compressedPayload = null;
        Integer compressedPayloadPayloadLength = null;
        try {
            String payload = bodyToString(response.getBody());
            if(!payload.isBlank()) {
                compressedPayload = AppBase64Util.base64Encode(ZipUtil.zip(payload));
                compressedPayloadPayloadLength = compressedPayload.length();
            }
        } catch (IOException e) {
            log.error("Unzip error", e);
        }

        Integer status = null;
        try {
            status = response.getStatusCode().value();
        } catch (IOException e) {
            log.error("Retrieve status code error", e);
        }
        String executionTime = MDC.get(Constants.MDC_CLIENT_EXECUTION_TIME);

        ReEventDto target = ReMapper.INSTANCE.clone(source);
        target.setSottoTipoEvento(SottoTipoEventoEnum.RESP);
        target.setEsito(EsitoEnum.RICEVUTA); //FIXME RICEVUTA o RICEVUTA_KO
        target.setHttpHeaders(httpHeaders);
        target.setHttpCallRemoteAddress(null);
        target.setCompressedPayload(compressedPayload);
        target.setCompressedPayloadPayloadLength(compressedPayloadPayloadLength);
        target.setHttpStatusCode(status);
        target.setExecutionTimeMs(executionTime);
        return target;
    }


    private static String formatClientHeaders(HttpHeaders headers) {
        headers.forEach((s,h)->{
            headers.add(s, StringUtils.join(h,","));
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
        for(String headerName : response.getHeaderNames()){
            headers.addAll(headerName, response.getHeaders(headerName).stream().toList());
        }

        return formatHeaders(headers);
    }

    private static String formatHeaders(HttpHeaders headers) {
        Stream<String> stream = headers.entrySet().stream()
                .map((entry) -> {
                    String values = entry.getValue().stream().collect(Collectors.joining("\", \"","\"","\""));
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
