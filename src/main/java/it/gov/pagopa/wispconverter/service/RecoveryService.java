package it.gov.pagopa.wispconverter.service;

import com.google.gson.Gson;
import gov.telematici.pagamenti.ws.nodoperpa.ppthead.IntestazionePPT;
import gov.telematici.pagamenti.ws.pafornode.PaSendRTV2Request;
import it.gov.pagopa.gen.wispconverter.client.cache.model.ConnectionDto;
import it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto;
import it.gov.pagopa.wispconverter.controller.ReceiptController;
import it.gov.pagopa.wispconverter.controller.model.*;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.RTRetryRepository;
import it.gov.pagopa.wispconverter.repository.model.*;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptStatusEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptTypeEnum;
import it.gov.pagopa.wispconverter.secondary.RTRepositorySecondary;
import it.gov.pagopa.wispconverter.secondary.ReEventRepositorySecondary;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.service.model.session.CommonFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.session.RPTContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import it.gov.pagopa.wispconverter.util.*;
import jakarta.xml.soap.SOAPMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecoveryService {

    private static final String RPT_ACCETTATA_NODO = "RPT_ACCETTATA_NODO";

    private static final String STATUS_RT_SEND_SUCCESS = "RT_SEND_SUCCESS";

    private static final String RECOVERY_VALID_START_DATE = "2024-09-03";

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT_DAY = "yyyy-MM-dd";
    private static final List<String> blockedReceiptStatus = List.of(
            ReceiptStatusEnum.NOT_SENT.name(),
            ReceiptStatusEnum.REDIRECT.name(),
            ReceiptStatusEnum.SCHEDULED.name());

    private final RTRepositorySecondary rtRepository;

    private final ReEventRepositorySecondary reEventRepository;

    private final ReceiptService receiptService;

    private final RPTRequestRepository rptRequestRepository;

    private final RTRetryRepository rtRetryRepository;

    private final ReService reService;

    private final RPTExtractorService rptExtractorService;

    private final ConfigCacheService configCacheService;

    private final ServiceBusService serviceBusService;

    private final JaxbElementUtil jaxbElementUtil;

    @Value("${wisp-converter.cached-requestid-mapping.ttl.minutes:1440}")
    public Long requestIDMappingTTL;
    @Value("${wisp-converter.recovery.receipt-generation.wait-time.minutes:60}")
    public Long receiptGenerationWaitTime;
    @Value("${wisp-converter.apim.path}")
    private String apimPath;

    // Recover by IUV
    public RecoveryReceiptResponse recoverReceiptKOByIUV(String creditorInstitution, String iuv, String dateFrom, String dateTo) {
        // Query database for blocked Receipt in given timestamp with given IUV
        List<RTEntity> rtEntities = rtRepository.findByMidReceiptStatusInAndTimestampBetween(getDateFrom(dateFrom), getDateTo(dateTo), creditorInstitution, iuv);
        // For each entity call send receipt KO
        rtEntities.forEach(rtEntity -> callSendReceiptKO(rtEntity.getDomainId(), rtEntity.getIuv(), rtEntity.getCcp(), rtEntity.getSessionId()));
        return this.extractRecoveryReceiptResponse(rtEntities);
    }

    // Recover by CI (async)
    public RecoveryReceiptResponse recoverReceiptKOByCI(String creditorInstitution, String dateFrom, String dateTo) {
        // Query database for blocked Receipt in given timestamp with given domainId (-> creditorInstitution)
        List<RTEntity> rtEntities = rtRepository.findByMidReceiptStatusInAndTimestampBetween(getDateFrom(dateFrom), getDateTo(dateTo), creditorInstitution);
        // Future
        CompletableFuture<Boolean> executeRecovery = CompletableFuture.supplyAsync(() -> {
            rtEntities.forEach(rtEntity -> callSendReceiptKO(rtEntity.getDomainId(), rtEntity.getIuv(), rtEntity.getCcp(), rtEntity.getSessionId()));
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
        List<RTEntity> rtEntities = rtRepository.findByMidReceiptStatusInAndTimestampBetween(dateFrom.toString(), dateTo.toString());
        // For each entity call send receipt KO
        rtEntities.forEach(rtEntity -> callSendReceiptKO(rtEntity.getDomainId(), rtEntity.getIuv(), rtEntity.getCcp(), rtEntity.getSessionId()));
        return this.extractRecoveryReceiptResponse(rtEntities);
    }

    // missing redirect recovery
    public int recoverMissingRedirect(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        String dateFromString = dateFrom.format(DateTimeFormatter.ofPattern(DATE_FORMAT_DAY));
        String dateToString = dateTo.format(DateTimeFormatter.ofPattern(DATE_FORMAT_DAY));
        String dateFromTSString = dateFrom.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        String dateToTSString = dateTo.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        int sent = 0;

        List<SessionIdEntity> sessionsWithoutRedirect = reEventRepository.findSessionWithoutRedirect(dateFromTSString, dateToTSString);

        for (SessionIdEntity sessionIdEntity : sessionsWithoutRedirect) {
            String sessionId = sessionIdEntity.getSessionId();
            List<ReEventEntity> reEventList = reEventRepository.findBySessionIdAndStatus(dateFromString, dateToString, sessionId, RPT_ACCETTATA_NODO, 1);

            if (!reEventList.isEmpty()) {
                ReEventEntity reEvent = reEventList.get(0);
                String iuv = reEvent.getIuv();
                String ccp = reEvent.getCcp();
                String ci = reEvent.getDomainId();

                List<ReEventEntity> reEventsRT = reEventRepository.findBySessionIdAndStatus(dateFromString, dateToString, sessionId, STATUS_RT_SEND_SUCCESS, 1);
                if (reEventsRT.isEmpty()) {
                    this.callSendReceiptKO(ci, iuv, ccp, sessionId);
                    sent++;
                }
            }
        }

        return sent;
    }

    // call sendRTKoFromSessionId
    public void callSendReceiptKO(String ci, String iuv, String ccp, String sessionId) {
        MDC.put(Constants.MDC_BUSINESS_PROCESS, "recovery-receipt-ko");

        generateRE(Constants.PAA_INVIA_RT, null, InternalStepStatus.RT_START_RECONCILIATION_PROCESS, ci, iuv, ccp, sessionId);

        try {
            log.info("[WISP-Recovery][SEND-RECEIPT-KO] receipt-ko for ci = {}, iuv = {}, ccp = {}, sessionId = {}", ci, iuv, ccp, sessionId);
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

    public RecoveryReceiptReportResponse recoverReceiptToBeReSent(RecoveryReceiptRequest request) {

        RecoveryReceiptReportResponse response = RecoveryReceiptReportResponse.builder()
                .receiptStatus(new LinkedList<>())
                .build();

        MDCUtil.setSessionDataInfo("recovery-receipt-ondemand");

        gov.telematici.pagamenti.ws.papernodo.ObjectFactory objectFactory = new gov.telematici.pagamenti.ws.papernodo.ObjectFactory();
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

                // extract data from old paaInviaRT
                String oldReceipt = rtRequestEntity.getPayload();
                SOAPMessage msg = jaxbElementUtil.getMessage(oldReceipt);
                IntestazionePPT oldReceiptHeader = jaxbElementUtil.getHeader(msg, IntestazionePPT.class);
                String domainId = oldReceiptHeader.getIdentificativoDominio();
                String iuv = oldReceiptHeader.getIdentificativoUnivocoVersamento();
                String ccp = oldReceiptHeader.getCodiceContestoPagamento();

                // retrieve the needed RPT
                RPTContentDTO rpt = sessionData.getAllRPTs().stream()
                        .filter(rptContent ->
                                domainId.equals(rptContent.getRpt().getDomain().getDomainId()) &&
                                        iuv.equals(rptContent.getIuv()) &&
                                        ccp.equals(rptContent.getCcp())
                        )
                        .findFirst()
                        .orElseThrow(() -> new Exception("No valid RPT found for receipt with id " + receiptId));

                // Re-generate the RT payload in order to actualize values and structural errors
                // If newly-generated payload is not null, the data in 'receipt-rt' is updated with extracted data
                String newlyGeneratedPayload = regenerateReceiptPayload(rtRequestEntity.getPartitionKey(), rtRequestEntity.getReceiptType(), sessionData, rpt, objectFactory);

                // update entity from 'receipt' container with retry 0 and newly-generated payload
                rtRequestEntity.setRetry(0);
                rtRequestEntity.setPayload(newlyGeneratedPayload != null ? newlyGeneratedPayload : oldReceipt);
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

    private String regenerateReceiptPayload(String date, ReceiptTypeEnum receiptType, SessionDataDTO sessionData, RPTContentDTO rpt,
                                            gov.telematici.pagamenti.ws.papernodo.ObjectFactory objectFactory) throws IOException {
        String payload = null;
        CommonFieldsDTO commonFields = sessionData.getCommonFields();
        ReceiptStatusEnum receiptStatus = ReceiptStatusEnum.SCHEDULED;

        // if receipt is of KO type, the RT regeneration is straightforward using rpt and session data
        if (ReceiptTypeEnum.KO.equals(receiptType)) {

            it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto configData = configCacheService.getConfigData();
            payload = receiptService.generateKoRtFromSessionData(rpt.getRpt().getDomain().getDomainId(), rpt.getIuv(), rpt, commonFields, objectFactory, configData.getConfigurations(), receiptStatus);
        }

        // if receipt is of OK type, the RT regeneration is more complex because the paSendRTV2 request is needed and it can only be retrieved from RE event
        else {

            // first of all: get first occurrence of OK RT 'try-to-send' operation. If no event is found, no RT was sent.
            // TODO REMOVE LIMIT IN ORDER TO HANDLE MULTI-RPT CARTS
            List<ReEventEntity> events = reEventRepository.findBySessionIdAndStatus(date, date, sessionData.getCommonFields().getSessionId(), InternalStepStatus.POSITIVE_RT_TRY_TO_SEND_TO_CREDITOR_INSTITUTION.toString(), 1);
            if (!events.isEmpty()) { // TODO this for each event found (pay attention to multiple tries)

                // using the operation id extracted from retrieved event, get 'receipt-ok' request occurred and logged by INTERFACE event. Again, if no event is found no RT send was triggered.
                ReEventEntity event = events.get(0);
                Optional<ReEventEntity> interfaceReqEventOpt = reEventRepository.findFirstInterfaceRequest(date, date, ReceiptController.BP_RECEIPT_OK, event.getOperationId());
                if (interfaceReqEventOpt.isPresent()) {

                    // get the compressed payload from event and decompress it, parsing a well-formed request
                    String unzippedRequest = new String(ZipUtil.unzip(AppBase64Util.base64Decode(interfaceReqEventOpt.get().getCompressedPayload())));
                    ReceiptRequest receiptOkRequest = new Gson().fromJson(unzippedRequest, ReceiptRequest.class);

                    // now, from request the paSendRTV2 content can be extracted
                    SOAPMessage envelopeElement = jaxbElementUtil.getMessage(receiptOkRequest.getContent());
                    PaSendRTV2Request paSendRTV2 = jaxbElementUtil.getBody(envelopeElement, PaSendRTV2Request.class);

                    // finally, use the extracted paSendRTV2 content to re-generate paaInviaRT request
                    IntestazionePPT intestazionePPT = ReceiptService.generateHeader(
                            paSendRTV2.getIdPA(),
                            paSendRTV2.getReceipt().getCreditorReferenceId(),
                            rpt.getRpt().getTransferData().getCcp(),
                            commonFields.getCreditorInstitutionBrokerId(),
                            commonFields.getStationId()
                    );
                    payload = receiptService.generateOkRtFromSessionData(rpt, paSendRTV2, intestazionePPT, commonFields, objectFactory, receiptStatus);
                }
            }
        }
        return payload;
    }
}