package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.controller.ReceiptController;
import it.gov.pagopa.wispconverter.controller.model.RecoveryReceiptPaymentResponse;
import it.gov.pagopa.wispconverter.controller.model.RecoveryReceiptResponse;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.RTRepository;
import it.gov.pagopa.wispconverter.repository.ReEventRepository;
import it.gov.pagopa.wispconverter.repository.model.RTEntity;
import it.gov.pagopa.wispconverter.repository.model.ReEventEntity;
import it.gov.pagopa.wispconverter.repository.model.SessionIdEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecoveryService {

    private static final String EVENT_TYPE_FOR_RECEIPTKO_SEARCH = "GENERATED_CACHE_ABOUT_RPT_FOR_RT_GENERATION";

    private static final String STATUS_RT_SEND_SUCCESS = "RT_SEND_SUCCESS";

    private static final String RECOVERY_VALID_START_DATE = "2024-09-03";

    private static final String MOCK_NOTICE_NUMBER = "348000000000000000";

    private static final List<String> BUSINESS_PROCESSES = List.of("receipt-ok", "receipt-ko", "ecommerce-hang-timeout-trigger");

    private final ReceiptController receiptController;

    private final RTRepository rtRepository;

    private final ReEventRepository reEventRepository;

    private final CacheRepository cacheRepository;

    private final ReService reService;

    @Value("${wisp-converter.cached-requestid-mapping.ttl.minutes}")
    private Long requestIDMappingTTL;

    @Value("${wisp-converter.recovery.receipt-generation.wait-time.minutes:60}")
    public Long receiptGenerationWaitTime;

    public RecoveryReceiptResponse recoverReceiptKOForCreditorInstitution(String creditorInstitution, String dateFrom, String dateTo) {

        LocalDate lowerLimit = LocalDate.parse(RECOVERY_VALID_START_DATE, DateTimeFormatter.ISO_LOCAL_DATE);
        if (LocalDate.parse(dateFrom, DateTimeFormatter.ISO_LOCAL_DATE).isBefore(lowerLimit)) {
            throw new AppException(AppErrorCodeMessageEnum.ERROR, String.format("The lower bound cannot be lower than [%s]", RECOVERY_VALID_START_DATE));
        }

        LocalDate now = LocalDate.now();
        LocalDate parse = LocalDate.parse(dateTo, DateTimeFormatter.ISO_LOCAL_DATE);
        if (parse.isAfter(now)) {
            throw new AppException(AppErrorCodeMessageEnum.ERROR, String.format("The upper bound cannot be higher than [%s]", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        }

        String dateToRefactored;
        if (now.isEqual(parse)) {
            ZonedDateTime nowMinusMinutes = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(receiptGenerationWaitTime);
            dateToRefactored = nowMinusMinutes.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("Upper bound forced to {}", dateToRefactored);
        } else {
            dateToRefactored = dateTo + " 23:59:59";
            log.info("Upper bound set to {}", dateToRefactored);
        }

        List<RTEntity> receiptRTs = rtRepository.findByOrganizationId(creditorInstitution, dateFrom, dateToRefactored);
        List<RecoveryReceiptPaymentResponse> paymentsToReconcile = receiptRTs.stream().map(entity -> RecoveryReceiptPaymentResponse.builder()
                        .iuv(entity.getIuv())
                        .ccp(entity.getCcp())
                        .ci(entity.getIdDominio())
                        .build())
                .toList();

        CompletableFuture<Boolean> executeRecovery = recoverReceiptKOAsync(dateFrom, dateTo, paymentsToReconcile);
        executeRecovery
                .thenAccept(value -> log.info("Reconciliation for creditor institution [{}] in date range [{}-{}] completed!", creditorInstitution, dateFrom, dateTo))
                .exceptionally(e -> {
                    log.error("Reconciliation for creditor institution [{}] in date range [{}-{}] ended unsuccessfully!", creditorInstitution, dateFrom, dateTo, e);
                    throw new AppException(e, AppErrorCodeMessageEnum.ERROR, e.getMessage());
                });

        return RecoveryReceiptResponse.builder()
                .payments(paymentsToReconcile)
                .build();
    }

    public int recoverReceiptKOAll(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        String dateFromString = dateFrom.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String dateToString = dateTo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<RTEntity> receiptRTs = rtRepository.findPendingRT(dateFromString, dateToString);
        List<RecoveryReceiptPaymentResponse> paymentsToReconcile = receiptRTs.stream().map(entity -> RecoveryReceiptPaymentResponse.builder()
                                                                                                             .iuv(entity.getIuv())
                                                                                                             .ccp(entity.getCcp())
                                                                                                             .ci(entity.getIdDominio())
                                                                                                             .build())
                                                                           .toList();

        recoverReceiptKOByRecoveryPayment(dateFromString, dateToString, paymentsToReconcile);

        return RecoveryReceiptResponse.builder()
                       .payments(paymentsToReconcile)
                       .build()
                       .getPayments()
                       .size();
    }

    public int recoverMissingRedirect(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        String dateFromString = dateFrom.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String dateToString = dateTo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<SessionIdEntity> sessionsWithoutRedirect = reEventRepository.findSessionWithoutRedirect(dateFromString, dateToString);

        for(SessionIdEntity sessionIdEntity : sessionsWithoutRedirect) {
            String sessionId = sessionIdEntity.getSessionId();
            List<ReEventEntity> reEventList = reEventRepository.findBySessionIdAndStatus(dateFromString, dateToString, sessionId, "RPT_ACCETTATA_NODO");

            if(!reEventList.isEmpty()) {
                ReEventEntity reEvent = reEventList.get(0);
                String iuv = reEvent.getIuv();
                String ccp = reEvent.getCcp();
                // String noticeNumber = reEvent.getNoticeNumber(); always null in RPT_ACCETTATA_NODO, the NAV is created after /redirect call!
                String ci = reEvent.getDomainId();

                log.info("[RECOVER-MISSING-REDIRECT] Recovery with receipt-ko for ci = {}, iuv = {}, ccp = {}, sessionId = {}", ci, iuv, ccp, sessionId);
                this.recoverReceiptKO(ci, MOCK_NOTICE_NUMBER, iuv, sessionId, ccp, dateFromString, dateToString);
            }
        }

        return sessionsWithoutRedirect.size();
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
                    String noticeNumber = event.getNoticeNumber();
                    String sessionId = event.getSessionId();

                    log.info("[RECOVERY-MISSING-RT] Recovery with receipt-ko for ci = {}, iuv = {}, ccp = {}, sessionId = {}", ci, iuv, ccp, sessionId);
                    this.recoverReceiptKO(ci, noticeNumber, iuv, sessionId, ccp, dateFrom, dateTo);
                }
            } catch (Exception e) {
                generateRE(Constants.PAA_INVIA_RT, "Failure", InternalStepStatus.RT_END_RECONCILIATION_PROCESS, ci, iuv, null, ccp, null);
                throw new AppException(e, AppErrorCodeMessageEnum.ERROR, e.getMessage());
            }
        }

        return true;
    }

    // check if there is a successful RT submission, if there isn't prepare cached data and send receipt-ko
    private void recoverReceiptKO(String ci, String noticeNumber, String iuv, String sessionId, String ccp, String dateFrom, String dateTo) {
        // search by sessionId, then filter by status=RT_SEND_SUCCESS. If there is zero, then proceed
        List<ReEventEntity> reEventsRT = reEventRepository.findBySessionIdAndStatus(dateFrom, dateTo, sessionId, STATUS_RT_SEND_SUCCESS);

        if (reEventsRT.isEmpty()) {
            String navToIuvMapping = String.format(DecouplerService.MAP_CACHING_KEY_TEMPLATE, ci, noticeNumber);
            String iuvToSessionIdMapping = String.format(DecouplerService.CACHING_KEY_TEMPLATE, ci, iuv);
            this.cacheRepository.insert(navToIuvMapping, iuvToSessionIdMapping, this.requestIDMappingTTL, ChronoUnit.MINUTES,true);
            this.cacheRepository.insert(iuvToSessionIdMapping, sessionId, this.requestIDMappingTTL, ChronoUnit.MINUTES,true);

            MDC.put(Constants.MDC_BUSINESS_PROCESS, "receipt-ko");
            generateRE(Constants.PAA_INVIA_RT, null, InternalStepStatus.RT_START_RECONCILIATION_PROCESS, ci, iuv, noticeNumber, ccp, sessionId);
            String receiptKoRequest = ReceiptDto.builder()
                                              .fiscalCode(ci)
                                              .noticeNumber(noticeNumber)
                                              .build()
                                              .toString();
            try {
                this.receiptController.receiptKo(receiptKoRequest);
            } catch (Exception e) {
                generateRE(Constants.PAA_INVIA_RT, "Failure", InternalStepStatus.RT_END_RECONCILIATION_PROCESS, ci, iuv, noticeNumber, ccp, sessionId);
                throw new AppException(e, AppErrorCodeMessageEnum.ERROR, e.getMessage());
            }
            generateRE(Constants.PAA_INVIA_RT, "Success", InternalStepStatus.RT_END_RECONCILIATION_PROCESS, ci, iuv, noticeNumber, ccp, sessionId);
            MDC.remove(Constants.MDC_BUSINESS_PROCESS);
        }
    }

    private void generateRE(String primitive, String operationStatus, InternalStepStatus status, String domainId, String iuv, String noticeNumber, String ccp, String sessionId) {

        // setting data in MDC for next use
        ReEventDto reEvent = ReUtil.getREBuilder()
                .primitive(primitive)
                .operationStatus(operationStatus)
                .status(status)
                .sessionId(sessionId)
                .domainId(domainId)
                .iuv(iuv)
                .ccp(ccp)
                .noticeNumber(noticeNumber)
                .build();
        reService.addRe(reEvent);
    }
}
