package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.ReEventRepository;
import it.gov.pagopa.wispconverter.repository.model.ReEventEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.OutcomeEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import it.gov.pagopa.wispconverter.service.mapper.ReEventMapper;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.service.model.re.ReRequestContext;
import it.gov.pagopa.wispconverter.service.model.re.ReResponseContext;
import it.gov.pagopa.wispconverter.util.AppBase64Util;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ZipUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReService {

    private final ReEventRepository reEventRepository;
    private final ReEventMapper reEventMapper;


    /**
     * @param status the event to send
     */
    public void sendEvent(WorkflowStatus status) {
        sendEvent(status, null, null, null, null);
    }

    /**
     * @param status           the event to send
     * @param info             some string to log
     */
    public void sendEvent(WorkflowStatus status,  String info) {
        sendEvent(status, info, null, null, null);
    }

    /**
     * @param status           the event to send
     * @param info             some string to log
     * @param outcome          the outcome of the event
     */
    public void sendEvent(
            WorkflowStatus status,  String info, OutcomeEnum outcome) {
        sendEvent(status,  info, outcome, null, null);
    }

    /**
     * @param status           the event to send
     * @param info             some string to log
     * @param outcome          the outcome of the event
     * @param request          the request to put in the event
     * @param response         the response to put in the event
     */
    public void sendEvent(
            WorkflowStatus status,
            @Nullable String info,
            @Nullable OutcomeEnum outcome,
            @Nullable ReRequestContext request,
            @Nullable ReResponseContext response) {

        try {
            Instant mdcStartTime = getStartTime();

            // build event
            var reEvent =
                    ReEventDto.builder()
                            //  context
                            .id(UUID.randomUUID().toString())
                            .operationId(MDC.get(Constants.MDC_OPERATION_ID))
                            .insertedTimestamp(mdcStartTime)
                            .businessProcess(MDC.get(Constants.MDC_BUSINESS_PROCESS))
                            .sessionId(MDC.get(Constants.MDC_SESSION_ID))
                            .executionTimeMs(getExecutionTimeMs())
                            //  event
                            .status(status.name())
                            .eventCategory(status.getType())
                            .outcome(outcome != null ? outcome.name() : null)
                            .info(info)
                            //  payment
                            .cartId(MDC.get(Constants.MDC_CART_ID))
                            .iuv(MDC.get(Constants.MDC_IUV))
                            .noticeNumber(MDC.get(Constants.MDC_NOTICE_NUMBER))
                            .ccp(MDC.get(Constants.MDC_CCP))
                            .paymentToken(MDC.get(Constants.MDC_PAYMENT_TOKEN))
                            .domainId(MDC.get(Constants.MDC_DOMAIN_ID))
                            .psp(MDC.get(Constants.MDC_PSP_ID))
                            .station(MDC.get(Constants.MDC_STATION_ID))
                            .channel(MDC.get(Constants.MDC_CHANNEL_ID))
                            //  error
                            .operationErrorDetail(MDC.get(Constants.MDC_ERROR_DETAIL))
                            .operationErrorCode(MDC.get(Constants.MDC_ERROR_CODE));

            //  request
            if (request != null) {
                reEvent
                        .httpMethod(request.getMethod().name())
                        .httpUri(request.getUri())
                        .requestHeaders(formatHeaders(request.getHeaders()))
                        .requestPayload(compressData(request.getPayload()));
            }
            //  response
            if (response != null) {
                reEvent
                        .responseHeaders(formatHeaders(response.getHeaders()))
                        .responsePayload(compressData(response.getPayload()))
                        .httpStatusCode(response.getStatusCode().value());
            }
            addRe(reEvent.build());
        } catch (Exception e) {
            throw new AppException(AppErrorCodeMessageEnum.PERSISTENCE_SAVING_RE_ERROR, e);
        }
    }

    private static Long getExecutionTimeMs() {
        try {
            return Long.valueOf(MDC.get(Constants.MDC_CLIENT_EXECUTION_TIME));
        } catch (IllegalArgumentException e) {
            return -1L;
        }
    }

    public void addRe(ReEventDto reEventDto) {
        ReEventEntity reEventEntity = reEventMapper.toReEventEntity(reEventDto);
        reEventEntity.setOperationErrorLine(MDC.get(Constants.MDC_ERROR_LINE));
        reEventRepository.save(reEventEntity);
    }

    private static Instant getStartTime() {
        return MDC.get(Constants.MDC_START_TIME) == null
                ? null
                : Instant.ofEpochMilli(Long.parseLong(MDC.get(Constants.MDC_START_TIME)));
    }

    private static String formatHeaders(HttpHeaders headers) {
        Stream<String> stream =
                headers.entrySet().stream()
                        .map(
                                entry -> {
                                    String values =
                                            entry.getValue().stream().collect(Collectors.joining("\", \"", "\"", "\""));
                                    return entry.getKey() + ": [" + values + "]";
                                });
        return stream.collect(Collectors.joining(", "));
    }

    private static String compressData(String payload) {
        String result = null;
        try {
            if (payload != null) {
                result = AppBase64Util.base64Encode(ZipUtil.zip(payload));
            }
        } catch (IOException e) {
            throw new AppException(AppErrorCodeMessageEnum.ERROR, e);
        }
        return result;
    }
}
