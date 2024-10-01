package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.gen.wispconverter.client.cache.model.ConnectionDto;
import it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto;
import it.gov.pagopa.wispconverter.controller.model.RecoveryProxyReceiptRequest;
import it.gov.pagopa.wispconverter.controller.model.RecoveryProxyReceiptResponse;
import it.gov.pagopa.wispconverter.controller.model.RecoveryReceiptPaymentResponse;
import it.gov.pagopa.wispconverter.controller.model.RecoveryReceiptResponse;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.*;
import it.gov.pagopa.wispconverter.repository.model.*;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptStatusEnum;
import it.gov.pagopa.wispconverter.secondary.RTRepositorySecondary;
import it.gov.pagopa.wispconverter.secondary.ReEventRepositorySecondary;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.MDCUtil;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecoveryService {

    private static final String RPT_ACCETTATA_NODO = "RPT_ACCETTATA_NODO";

    private static final String STATUS_RT_SEND_SUCCESS = "RT_SEND_SUCCESS";

    private static final String RECOVERY_VALID_START_DATE = "2024-09-03";

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final RTRepositorySecondary rtRepository;

    private final ReEventRepositorySecondary reEventRepository;

    private static final String DATE_FORMAT_DAY = "yyyy-MM-dd";

  private static final List<String> blockedReceiptStatus =
      List.of(
              ReceiptStatusEnum.NOT_SENT.name(),
              ReceiptStatusEnum.REDIRECT.name(),
              ReceiptStatusEnum.SCHEDULED.name());

    private final ReceiptService receiptService;
    private final RPTRequestRepository rptRequestRepository;
    private final RTRetryRepository rtRetryRepository;
    private final ReService reService;
    private final RPTExtractorService rptExtractorService;
    private final ConfigCacheService configCacheService;
    private final ServiceBusService serviceBusService;

    @Value("${wisp-converter.cached-requestid-mapping.ttl.minutes:1440}")
    public Long requestIDMappingTTL;

    @Value("${wisp-converter.apim.path}")
    private String apimPath;

    @Value("${wisp-converter.recovery.receipt-generation.wait-time.minutes:60}")
    public Long receiptGenerationWaitTime;

    // Recover by IUV
    public RecoveryReceiptResponse recoverReceiptKOByIUV(String creditorInstitution, String iuv, String dateFrom, String dateTo) {
        // Query database for blocked Receipt in given timestamp with given IUV
        List<RTEntity> rtEntities = rtRepository.findByBlockedReceiptStatusInAndTimestampBetween(getDateFrom(dateFrom), getDateTo(dateTo), creditorInstitution, iuv);
        // For each entity call send receipt KO
        rtEntities.forEach(rtEntity -> callSendReceiptKO(rtEntity.getDomainId(), rtEntity.getIuv(), rtEntity.getSessionId(), rtEntity.getCcp()));
        return this.extractRecoveryReceiptResponse(rtEntities);
    }

    // Recover by CI (async)
    public RecoveryReceiptResponse recoverReceiptKOByCI(String creditorInstitution, String dateFrom, String dateTo) {
        MDCUtil.setSessionDataInfo("recovery-ci-receipt-ko");
        // Query database for blocked Receipt in given timestamp with given domainId (-> creditorInstitution)
        List<RTEntity> rtEntities = rtRepository.findByBlockedReceiptStatusInAndTimestampBetween(getDateFrom(dateFrom), getDateTo(dateTo), creditorInstitution);
        // Future
        CompletableFuture<Boolean> executeRecovery = CompletableFuture.supplyAsync(() -> {
            rtEntities.forEach(rtEntity -> callSendReceiptKO(rtEntity.getDomainId(), rtEntity.getIuv(), rtEntity.getSessionId(), rtEntity.getCcp()));
            return true;
        });
        executeRecovery
                .thenAccept(value -> log.debug("Reconciliation for creditor institution [{}] in date range [{}-{}] completed!", creditorInstitution, dateFrom, dateTo))
                .exceptionally(e -> {
                    log.error("Reconciliation for creditor institution [{}] in date range [{}-{}] ended unsuccessfully!", creditorInstitution, dateFrom, dateTo, e);
                    throw new AppException(e, AppErrorCodeMessageEnum.ERROR, e.getMessage());
                });
        // Returns the entities that will be recovered in async
        return extractRecoveryReceiptResponse(rtEntities);
    }

    // Recover by dates (cron)
    public RecoveryReceiptResponse recoverReceiptKOByDate(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        // Query database for blocked Receipt in given timestamp
        List<RTEntity> rtEntities = rtRepository.findByBlockedReceiptStatusInAndTimestampBetween(dateFrom.toString(), dateTo.toString());
        // For each entity call send receipt KO
        rtEntities.forEach(rtEntity -> callSendReceiptKO(rtEntity.getDomainId(), rtEntity.getIuv(), rtEntity.getSessionId(), rtEntity.getCcp()));
        return this.extractRecoveryReceiptResponse(rtEntities);
    }

    // missing redirect recovery
    public int recoverMissingRedirect(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        String dateFromString = dateFrom.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        String dateToString = dateTo.format(DateTimeFormatter.ofPattern(DATE_FORMAT));

        List<SessionIdEntity> sessionsWithoutRedirect = reEventRepository.findSessionWithoutRedirect(dateFromString, dateToString);

        for(SessionIdEntity sessionIdEntity : sessionsWithoutRedirect) {
            String sessionId = sessionIdEntity.getSessionId();
            List<ReEventEntity> reEventList = reEventRepository.findBySessionIdAndStatus(dateFromString, dateToString, sessionId, RPT_ACCETTATA_NODO);

            if(!reEventList.isEmpty()) {
                ReEventEntity reEvent = reEventList.get(0);
                String iuv = reEvent.getIuv();
                String ccp = reEvent.getCcp();
                String ci = reEvent.getDomainId();

                log.info("[RECOVER-MISSING-REDIRECT] Recovery with receipt-ko for ci = {}, iuv = {}, ccp = {}, sessionId = {}", ci, iuv, ccp, sessionId);
                // search by sessionId, then filter by status=RT_SEND_SUCCESS. If there is zero, then proceed
                List<ReEventEntity> reEventsRT = reEventRepository.findBySessionIdAndStatus(dateFromString, dateToString, sessionId, STATUS_RT_SEND_SUCCESS);
                if(reEventsRT.isEmpty())
                    this.callSendReceiptKO(ci, iuv, ccp, sessionId);
            }
        }

        return sessionsWithoutRedirect.size();
    }

    // call sendRTKoFromSessionId
    public void callSendReceiptKO(String ci, String iuv, String ccp, String sessionId) {
        MDC.put(Constants.MDC_BUSINESS_PROCESS, "receipt-ko");

        generateRE(Constants.PAA_INVIA_RT, null, InternalStepStatus.RT_START_RECONCILIATION_PROCESS, ci, iuv, ccp, sessionId);

        try {
            this.receiptService.sendRTKoFromSessionId(sessionId, InternalStepStatus.NEGATIVE_RT_TRY_TO_SEND_TO_CREDITOR_INSTITUTION);
        } catch (Exception e) {
            generateRE(Constants.PAA_INVIA_RT, "Failure", InternalStepStatus.RT_END_RECONCILIATION_PROCESS, ci, iuv, ccp, sessionId);
            throw new AppException(e, AppErrorCodeMessageEnum.ERROR, e.getMessage());
        }
        generateRE(Constants.PAA_INVIA_RT, "Success", InternalStepStatus.RT_END_RECONCILIATION_PROCESS, ci, iuv, ccp, sessionId);
        MDC.remove(Constants.MDC_BUSINESS_PROCESS);
    }

    // checks date validity
    private String getDateFrom(String dateFrom) {
        LocalDate lowerLimit = LocalDate.parse(RECOVERY_VALID_START_DATE, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate dateFromLocalDate = LocalDate.parse(dateFrom, DateTimeFormatter.ISO_LOCAL_DATE);
        if (dateFromLocalDate.isBefore(lowerLimit)) {
            throw new AppException(AppErrorCodeMessageEnum.ERROR, String.format("The lower bound cannot be lower than [%s]", RECOVERY_VALID_START_DATE));
        }
        return dateFrom;
    }

    // checks date validity and adjusts the value if necessary
    private String getDateTo(String dateTo) {
        LocalDate now = LocalDate.now();
        String dateToRefactored;

        LocalDate dateToLocalDate = LocalDate.parse(dateTo, DateTimeFormatter.ISO_LOCAL_DATE);
        if (dateToLocalDate.isAfter(now)) {
            throw new AppException(AppErrorCodeMessageEnum.ERROR, String.format("The upper bound cannot be higher than [%s]", now.format(DateTimeFormatter.ofPattern(DATE_FORMAT_DAY))));
        }

        if (LocalDate.now().isEqual(dateToLocalDate)) {
            ZonedDateTime nowMinusMinutes = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(receiptGenerationWaitTime);
            dateToRefactored = nowMinusMinutes.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        } else {
            dateToRefactored = dateTo + " 23:59:59";
        }

        return dateToRefactored;
    }

    private RecoveryReceiptResponse extractRecoveryReceiptResponse(List<RTEntity> rtEntities) {
        List<RecoveryReceiptPaymentResponse> responses = new ArrayList<>();

        for (RTEntity rtEntity : rtEntities) {
            responses.add(
                    RecoveryReceiptPaymentResponse.builder()
                            .iuv(rtEntity.getIuv())
                            .ccp(rtEntity.getCcp())
                            .ci(rtEntity.getDomainId())
                            .build());
        }

        return RecoveryReceiptResponse.builder()
                       .payments(responses)
                       .build();
    }

    private void generateRE(String primitive, String operationStatus, InternalStepStatus status, String domainId, String iuv, String ccp, String sessionId) {

        // setting data in MDC for next use
        ReEventDto reEvent = ReUtil.getREBuilder()
                .primitive(primitive)
                .operationStatus(operationStatus)
                .status(status)
                .sessionId(sessionId)
                .domainId(domainId)
                .iuv(iuv)
                .ccp(ccp)
                .noticeNumber(null)
                .build();
        reService.addRe(reEvent);
    }

    public RecoveryProxyReceiptResponse recoverReceiptToBeSentByProxy(RecoveryProxyReceiptRequest request) {

        RecoveryProxyReceiptResponse response = RecoveryProxyReceiptResponse.builder()
                .receiptStatus(new LinkedList<>())
                .build();

        MDCUtil.setSessionDataInfo("recovery-receipt-without-proxy");
        for (String receiptId : request.getReceiptIds()) {

            String sessionId = null;
            try {
                Optional<RTRequestEntity> rtRequestEntityOpt = rtRetryRepository.findById(receiptId);
                if (rtRequestEntityOpt.isEmpty()) {
                    throw new AppException(AppErrorCodeMessageEnum.ERROR, String.format("No valid receipt found with id [%s]", receiptId));
                }

                RTRequestEntity rtRequestEntity = rtRequestEntityOpt.get();
                String idempotencyKey = rtRequestEntity.getIdempotencyKey();
                String[] idempotencyKeyComponents = idempotencyKey.split("_");
                if (idempotencyKeyComponents.length < 2) {
                    throw new AppException(AppErrorCodeMessageEnum.ERROR, String.format("Invalid idempotency key [%s]. It must be composed of sessionId, notice number and domain.", idempotencyKey));
                }

                sessionId = idempotencyKeyComponents[0];
                Optional<RPTRequestEntity> rptRequestOpt = rptRequestRepository.findById(sessionId);
                if (rptRequestOpt.isEmpty()) {
                    throw new AppException(AppErrorCodeMessageEnum.ERROR, String.format("No valid RPT request found with id [%s].", sessionId));
                }

                RPTRequestEntity rptRequestEntity = rptRequestOpt.get();
                SessionDataDTO sessionData = rptExtractorService.extractSessionData(rptRequestEntity.getPrimitive(), rptRequestEntity.getPayload());

                String stationId = sessionData.getCommonFields().getStationId();
                StationDto station = configCacheService.getStationByIdFromCache(stationId);

                ConnectionDto stationConnection = station.getConnection();
                URI uri = CommonUtility.constructUrl(
                        stationConnection.getProtocol().getValue(),
                        stationConnection.getIp(),
                        stationConnection.getPort().intValue(),
                        station.getService() != null ? station.getService().getPath() : ""
                );
                InetSocketAddress proxyAddress = CommonUtility.constructProxyAddress(uri, station, apimPath);
                if (proxyAddress != null) {
                    rtRequestEntity.setProxyAddress(String.format("%s:%s", proxyAddress.getHostString(), proxyAddress.getPort()));
                }
                rtRequestEntity.setRetry(0);
                rtRetryRepository.save(rtRequestEntity);

                String compositedIdForReceipt = String.format("%s_%s", rtRequestEntity.getPartitionKey(), rtRequestEntity.getId());
                serviceBusService.sendMessage(compositedIdForReceipt, null);
                generateRE(null, "Success", InternalStepStatus.RT_SEND_RESCHEDULING_SUCCESS, null, null, null, sessionId);
                response.getReceiptStatus().add(Pair.of(receiptId, "SCHEDULED"));

            } catch (Exception e) {

                log.error("Reconciliation for receipt id [{}] ended unsuccessfully!", receiptId, e);
                generateRE(Constants.PAA_INVIA_RT, "Failure", InternalStepStatus.RT_SEND_RESCHEDULING_FAILURE, null, null, null, sessionId);

                response.getReceiptStatus().add(Pair.of(receiptId, String.format("FAILED: [%s]", e.getMessage())));
            }
        }

        return response;
    }
}