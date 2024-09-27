package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.gen.wispconverter.client.cache.model.ConnectionDto;
import it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto;
import it.gov.pagopa.wispconverter.controller.model.RecoveryProxyReceiptRequest;
import it.gov.pagopa.wispconverter.controller.model.RecoveryProxyReceiptResponse;
import it.gov.pagopa.wispconverter.controller.model.RecoveryReceiptPaymentResponse;
import it.gov.pagopa.wispconverter.controller.model.RecoveryReceiptResponse;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.RTRepository;
import it.gov.pagopa.wispconverter.repository.RTRetryRepository;
import it.gov.pagopa.wispconverter.repository.ReEventRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.repository.model.RTEntity;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import it.gov.pagopa.wispconverter.repository.model.ReEventEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecoveryService {

    public static final String EVENT_TYPE_FOR_RECEIPTKO_SEARCH = "GENERATED_CACHE_ABOUT_RPT_FOR_RT_GENERATION";
    public static final String RPT_PARCHEGGIATA_NODO = "RPT_PARCHEGGIATA_NODO";
    private static final String RPT_ACCETTATA_NODO = "RPT_ACCETTATA_NODO";
    private static final String STATUS_RT_SEND_SUCCESS = "RT_SEND_SUCCESS";

    private static final String RECOVERY_VALID_START_DATE = "2024-09-03";

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final String DATE_FORMAT_DAY = "yyyy-MM-dd";

    private final ReceiptService receiptService;
    private final RPTRequestRepository rptRequestRepository;
    private final RTRepository rtRepository;
    private final RTRetryRepository rtRetryRepository;
    private final ReEventRepository reEventRepository;
    private final ReService reService;
    private final RPTExtractorService rptExtractorService;
    private final ConfigCacheService configCacheService;
    private final ServiceBusService serviceBusService;

    @Value("${wisp-converter.cached-requestid-mapping.ttl.minutes:1440}")
    public Long requestIDMappingTTL;
    @Value("${wisp-converter.recovery.receipt-generation.wait-time.minutes:60}")
    public Long receiptGenerationWaitTime;
    @Value("${wisp-converter.apim.path}")
    private String apimPath;

    private static Set<String> getWISPInterruptStatusSet() {
        return Set.of(
                RPT_ACCETTATA_NODO,
                RPT_PARCHEGGIATA_NODO,
                "GENERATED_NAV_FOR_NEW_PAYMENT_POSITION",
                "CREATED_NEW_PAYMENT_POSITION_IN_GPD",
                "GENERATED_CACHE_ABOUT_RPT_FOR_DECOUPLER",
                EVENT_TYPE_FOR_RECEIPTKO_SEARCH,
                "SAVED_RPT_IN_CART_RECEIVED_REDIRECT_URL_FROM_CHECKOUT",
                "GENERATED_CACHE_ABOUT_RPT_FOR_CARTSESSION_CACHING"
        );
    }

    public boolean recoverReceiptKO(String creditorInstitution, String iuv, String dateFrom, String dateTo) {
        checkDateValidity(dateFrom, dateTo);
        List<ReEventEntity> reEvents = reEventRepository.findByIuvAndOrganizationId(dateFrom, dateTo, iuv, creditorInstitution)
                .stream()
                .sorted(Comparator.comparing(ReEventEntity::getInsertedTimestamp))
                .toList();

        Set<String> interruptStatusSet = getWISPInterruptStatusSet();

        if (!reEvents.isEmpty()) {
            ReEventEntity lastEvent = reEvents.get(0);
            String status = lastEvent.getStatus();
            if (status != null && interruptStatusSet.contains(status)) {
                this.recoverReceiptKO(creditorInstitution, iuv, lastEvent.getSessionId(), lastEvent.getCcp(), dateFrom, dateTo);
                return true;
            }
        }

        return false;
    }

    public RecoveryReceiptResponse recoverReceiptKOForCreditorInstitution(String creditorInstitution, String dateFrom, String dateTo) {
        MDCUtil.setSessionDataInfo("recovery-receipt-ko");

        checkDateValidity(dateFrom, dateTo);

        LocalDate parse = LocalDate.parse(dateTo, DateTimeFormatter.ISO_LOCAL_DATE);
        String dateToRefactored;

        if (LocalDate.now().isEqual(parse)) {
            ZonedDateTime nowMinusMinutes = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(receiptGenerationWaitTime);
            dateToRefactored = nowMinusMinutes.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
            log.info("Upper bound forced to {}", dateToRefactored);
        } else {
            dateToRefactored = dateTo + " 23:59:59";
            log.info("Upper bound set to {}", dateToRefactored);
        }

        List<RTEntity> receiptRTs = rtRepository.findByOrganizationId(creditorInstitution, dateFrom, dateToRefactored);
        List<RecoveryReceiptPaymentResponse> paymentsToReconcile = receiptRTs.stream().map(entity -> RecoveryReceiptPaymentResponse.builder()
                        .iuv(entity.getIuv())
                        .ccp(entity.getCcp())
                        .ci(entity.getDomainId())
                        .build())
                .toList();

        CompletableFuture<Boolean> executeRecovery = recoverReceiptKOAsync(dateFrom, dateTo, paymentsToReconcile);
        executeRecovery
                .thenAccept(value -> log.debug("Reconciliation for creditor institution [{}] in date range [{}-{}] completed!", creditorInstitution, dateFrom, dateTo))
                .exceptionally(e -> {
                    log.error("Reconciliation for creditor institution [{}] in date range [{}-{}] ended unsuccessfully!", creditorInstitution, dateFrom, dateTo, e);
                    throw new AppException(e, AppErrorCodeMessageEnum.ERROR, e.getMessage());
                });

        return RecoveryReceiptResponse.builder()
                .payments(paymentsToReconcile)
                .build();
    }

    private CompletableFuture<Boolean> recoverReceiptKOAsync(String dateFrom, String dateTo, List<RecoveryReceiptPaymentResponse> paymentsToReconcile) {
        return CompletableFuture.supplyAsync(() -> recoverReceiptKOByRecoveryPayment(dateFrom, dateTo, paymentsToReconcile));
    }

    private boolean recoverReceiptKOByRecoveryPayment(String dateFrom, String dateTo, List<RecoveryReceiptPaymentResponse> paymentsToReconcile) {

        for (RecoveryReceiptPaymentResponse payment : paymentsToReconcile) {
            String iuv = payment.getIuv();
            String ccp = payment.getCcp();
            String ci = payment.getCi();

            try {
                List<ReEventEntity> reEvents = reEventRepository.findByIuvAndOrganizationId(dateFrom, dateTo, iuv, ci);

                List<ReEventEntity> filteredEvents = reEvents.stream()
                        .filter(event -> EVENT_TYPE_FOR_RECEIPTKO_SEARCH.equals(event.getStatus()))
                        .filter(event -> ccp.equals(event.getCcp()))
                        .sorted(Comparator.comparing(ReEventEntity::getInsertedTimestamp))
                        .toList();

                int numberOfEvents = filteredEvents.size();
                if (numberOfEvents > 0) {
                    ReEventEntity event = filteredEvents.get(numberOfEvents - 1);
                    String sessionId = event.getSessionId();

                    log.info("[RECOVERY-MISSING-RT] Recovery with receipt-ko for ci = {}, iuv = {}, ccp = {}, sessionId = {}", ci, iuv, ccp, sessionId);
                    this.recoverReceiptKO(ci, iuv, ccp, sessionId, dateFrom, dateTo);
                }
            } catch (Exception e) {
                generateRE(Constants.PAA_INVIA_RT, "Failure", InternalStepStatus.RT_END_RECONCILIATION_PROCESS, ci, iuv, ccp, null);
                throw new AppException(e, AppErrorCodeMessageEnum.ERROR, e.getMessage());
            }
        }

        return true;
    }

    // check if there is a successful RT submission, if there isn't prepare cached data and send receipt-ko
    public void recoverReceiptKO(String ci, String iuv, String ccp, String sessionId, String dateFrom, String dateTo) {
        // search by sessionId, then filter by status=RT_SEND_SUCCESS. If there is zero, then proceed
        List<ReEventEntity> reEventsRT = reEventRepository.findBySessionIdAndStatus(dateFrom, dateTo, sessionId, STATUS_RT_SEND_SUCCESS);

        if (reEventsRT.isEmpty()) {
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
    }

    private void checkDateValidity(String dateFrom, String dateTo) {
        LocalDate lowerLimit = LocalDate.parse(RECOVERY_VALID_START_DATE, DateTimeFormatter.ISO_LOCAL_DATE);
        if (LocalDate.parse(dateFrom, DateTimeFormatter.ISO_LOCAL_DATE).isBefore(lowerLimit)) {
            throw new AppException(AppErrorCodeMessageEnum.ERROR, String.format("The lower bound cannot be lower than [%s]", RECOVERY_VALID_START_DATE));
        }

        LocalDate today = LocalDate.now();
        LocalDate parse = LocalDate.parse(dateTo, DateTimeFormatter.ISO_LOCAL_DATE);
        if (parse.isAfter(today)) {
            throw new AppException(AppErrorCodeMessageEnum.ERROR, String.format("The upper bound cannot be higher than [%s]", today.format(DateTimeFormatter.ofPattern(DATE_FORMAT_DAY))));
        }
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
                if (idempotencyKeyComponents.length == 3) {
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