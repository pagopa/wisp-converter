package it.gov.pagopa.wispconverter.servicebus;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.telematici.pagamenti.ws.nodoperpa.ppthead.IntestazionePPT;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.ReceiptDeadLetterRepository;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import it.gov.pagopa.wispconverter.repository.model.ReceiptDeadLetterEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.*;
import it.gov.pagopa.wispconverter.service.*;
import it.gov.pagopa.wispconverter.util.*;
import jakarta.xml.soap.SOAPMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

@Component
@Slf4j
public class RTConsumer extends SBConsumer {
    private final ObjectMapper mapper = new ObjectMapper();
    @Value("${wisp-converter.rt-send.max-retries:5}")
    private Integer maxRetries;
    @Value("${wisp-converter.rt-send.scheduling-time-in-minutes:60}")
    private Integer schedulingTimeInMinutes;
    @Value("${azure.sb.wisp-paainviart-queue.connectionString}")
    private String connectionString;
    @Value("${azure.sb.paaInviaRT.name}")
    private String queueName;
    @Autowired
    private RtRetryComosService rtRetryComosService;

    @Autowired
    private RtReceiptCosmosService rtReceiptCosmosService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private PaaInviaRTSenderService paaInviaRTSenderService;

    @Autowired
    private ServiceBusService serviceBusService;

    @Autowired
    private ReceiptDeadLetterRepository receiptDeadLetterRepository;

    @Autowired
    private ReService reService;

    @Autowired
    private JaxbElementUtil jaxbElementUtil;

    @Value("${disable-service-bus-receiver}")
    private boolean disableServiceBusReceiver;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeClient() {
        if (receiverClient != null && !disableServiceBusReceiver) {
            log.info("[Scheduled] Starting RTConsumer {}", ZonedDateTime.now());
            receiverClient.start();
        }
    }

    @PostConstruct
    public void post() {
        if (StringUtils.isNotBlank(connectionString)
                && !connectionString.equals("-")
                && !disableServiceBusReceiver) {
            receiverClient =
                    CommonUtility.getServiceBusProcessorClient(
                            connectionString, queueName, this::processMessage, this::processError);
        }
    }

    public void processMessage(ServiceBusReceivedMessageContext context) {
        // retrieving content from context of arrived message
        MDCUtil.setSessionDataInfo("resend-rt");
        ServiceBusReceivedMessage message = context.getMessage();
        log.debug("Processing " + message.getMessageId());

        // extracting the values needed for the search of the receipt persisted in storage
        String compositedIdForReceipt = new String(message.getBody().toBytes());
        String[] idSections = compositedIdForReceipt.split("_");
        String rtInsertionDate = idSections[0];
        String receiptId = idSections[1] + "_" + idSections[2];

        // get RT request entity from database
        RTRequestEntity rtRequestEntity = rtRetryComosService.getRTRequestEntity(receiptId, rtInsertionDate);

        MDC.put(Constants.MDC_SESSION_ID, rtRequestEntity.getSessionId());
        MDC.put(Constants.MDC_CCP, rtRequestEntity.getCcp());
        MDC.put(Constants.MDC_DOMAIN_ID, rtRequestEntity.getDomainId());
        MDC.put(Constants.MDC_IUV, rtRequestEntity.getIuv());
        MDC.put(Constants.MDC_STATION_ID, rtRequestEntity.getStation());

        String idempotencyKey = rtRequestEntity.getIdempotencyKey();
        ReceiptTypeEnum receiptType = rtRequestEntity.getReceiptType();

        IdempotencyStatusEnum idempotencyStatus = IdempotencyStatusEnum.FAILED;
        boolean isIdempotencyKeyProcessable = false;
        OutcomeEnum outcome = OutcomeEnum.ERROR;
        try {

            // before sending the RT to the creditor institution, the idempotency key must be checked in
            // order to not send duplicated receipts
            isIdempotencyKeyProcessable = idempotencyService.isIdempotencyKeyProcessable(idempotencyKey, receiptType);
            if (isIdempotencyKeyProcessable) {

                // Lock idempotency key status to avoid concurrency issues
                idempotencyService.lockIdempotencyKey(idempotencyKey, receiptType);

                // If receipt was found, it must be sent to creditor institution, so it try this operation
                log.debug("Sending message {}, retry: {}", compositedIdForReceipt, rtRequestEntity.getRetry());
                boolean isSend = resendRTToCreditorInstitution(receiptId, rtRequestEntity, compositedIdForReceipt, idempotencyKey);

                idempotencyStatus = isSend ? IdempotencyStatusEnum.SUCCESS : IdempotencyStatusEnum.FAILED;

            } else {

                // Status was locked due to concurrent execution, so it will be retried at the next
                // execution (but only if it is not completed)
                if (!idempotencyService.isCompleted(idempotencyKey)) {
                    reScheduleReceiptSend(rtRequestEntity, receiptId, compositedIdForReceipt);
                }
            }

            outcome = MDC.get(Constants.MDC_OUTCOME) == null ? OutcomeEnum.OK : OutcomeEnum.valueOf(MDC.get(Constants.MDC_OUTCOME));

        } catch (Exception e) {
            outcome = MDC.get(Constants.MDC_OUTCOME) == null ? OutcomeEnum.ERROR : OutcomeEnum.valueOf(MDC.get(Constants.MDC_OUTCOME));
        } finally {
            unlockIdempotencyKey(isIdempotencyKeyProcessable, idempotencyKey, receiptType, idempotencyStatus);
            reService.sendEvent(WorkflowStatus.RECEIPT_RESEND_PROCESSED, null, outcome);
        }

    }

    private void unlockIdempotencyKey(boolean isIdempotencyKeyProcessable, String idempotencyKey, ReceiptTypeEnum receiptType, IdempotencyStatusEnum idempotencyStatus) {
        // Unlock idempotency key after a successful operation
        if (isIdempotencyKeyProcessable) {
            try {
                idempotencyService.unlockIdempotencyKey(idempotencyKey, receiptType, idempotencyStatus);
            } catch (AppException e) {
                log.error("AppException: ", e);
            }
        }
    }

    private boolean resendRTToCreditorInstitution(
            String receiptId,
            RTRequestEntity receipt,
            String compositedIdForReceipt,
            String idempotencyKey) {

        boolean isSend = false;
        String ci = receipt.getDomainId();
        String iuv = receipt.getIuv();
        String ccp = receipt.getCcp();

        try {

            log.debug("Sending receipt [{}]", receiptId);

            // unzip retrieved zipped payload from GZip format
            byte[] unzippedPayload = ZipUtil.unzip(AppBase64Util.base64Decode(receipt.getPayload()));
            SOAPMessage envelopeElement = jaxbElementUtil.getMessage(unzippedPayload);
            IntestazionePPT header = jaxbElementUtil.getHeader(envelopeElement, IntestazionePPT.class);

            // set MDC session data for RE
            String noticeNumberFromIdempotencyKey = null;
            String[] idempotencyKeySections = idempotencyKey.split("_");
            if (idempotencyKeySections.length > 1 && !"null".equals(idempotencyKeySections[1])) {
                noticeNumberFromIdempotencyKey = idempotencyKeySections[1];
            }
            MDCUtil.setSessionDataInfo(header, noticeNumberFromIdempotencyKey);

            InetSocketAddress proxyAddress = null;
            if (receipt.getProxyAddress() != null) {
                String[] proxyComponents = receipt.getProxyAddress().split(":");
                if (proxyComponents.length != 2) {
                    throw new AppException(AppErrorCodeMessageEnum.CONFIGURATION_INVALID_STATION_PROXY);
                }
                proxyAddress =
                        new InetSocketAddress(proxyComponents[0], Integer.parseInt(proxyComponents[1]));
            }

            String rawPayload = new String(unzippedPayload);

            paaInviaRTSenderService.sendToCreditorInstitution(
                    URI.create(receipt.getUrl()),
                    proxyAddress,
                    extractHeaders(receipt.getHeaders()),
                    rawPayload,
                    ci,
                    iuv,
                    ccp);
            rtRetryComosService.deleteRTRequestEntity(receipt);
            log.debug("Sent receipt [{}]", receiptId);


            isSend = true;

        } catch (AppException e) {

            if (e.getError().equals(AppErrorCodeMessageEnum.RECEIPT_GENERATION_ERROR_DEAD_LETTER)) {
                MDC.put(Constants.MDC_OUTCOME, OutcomeEnum.SENDING_RT_FAILED_STORED_IN_DEADLETTER.name());

                // Sending dead letter in case of unknown status
                receiptDeadLetterRepository.save(
                        mapper.convertValue(receipt, ReceiptDeadLetterEntity.class));

                // Remove receipt from receipt collection
                rtRetryComosService.deleteRTRequestEntity(receipt);
            } else {
                MDC.put(Constants.MDC_OUTCOME, OutcomeEnum.SENDING_RT_FAILED.name());

                // Rescheduled due to errors caused by faulty communication with creditor institution
                reScheduleReceiptSend(receipt, receiptId, compositedIdForReceipt);
            }

        } catch (IOException e) {
            MDC.put(Constants.MDC_OUTCOME, OutcomeEnum.SENDING_RT_FAILED.name());

            rtReceiptCosmosService.updateReceiptStatus(ci, iuv, ccp, ReceiptStatusEnum.NOT_SENT);

            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_ZIPPED_PAYLOAD);
        }
        return isSend;
    }

    private List<Pair<String, String>> extractHeaders(List<String> headers) {
        List<Pair<String, String>> headerPairs = new LinkedList<>();
        for (String rawHeader : headers) {
            String[] keys = rawHeader.split(":");
            if (keys.length == 2) {
                headerPairs.add(Pair.of(keys[0], keys[1]));
            }
        }
        return headerPairs;
    }

    private void reScheduleReceiptSend(
            RTRequestEntity receipt, String receiptId, String compositedIdForReceipt) {
        String ci = receipt.getDomainId();
        String iuv = receipt.getIuv();
        String ccp = receipt.getCcp();

        // because of the not sent receipt, it is necessary to schedule a retry of the sending process
        // for this receipt
        if (receipt.getRetry() < this.maxRetries - 1) {

            try {

                // if required, update the retry count for the retrieved RT
                log.debug("Increasing retry by one and saving receipt with id: [{}]", receiptId);
                receipt.setRetry(receipt.getRetry() + 1);
                rtRetryComosService.saveRTRequestEntity(receipt);

                // because of the not sent receipt, it is necessary to schedule a retry of the sending
                // process for this receipt
                serviceBusService.sendMessage(compositedIdForReceipt, schedulingTimeInMinutes);

                rtReceiptCosmosService.updateReceiptStatus(ci, iuv, ccp, ReceiptStatusEnum.SCHEDULED);

                MDC.put(Constants.MDC_OUTCOME, OutcomeEnum.SENDING_RT_FAILED_RESCHEDULING_SUCCESSFUL.name());

            } catch (Exception e) {

                MDC.put(Constants.MDC_OUTCOME, OutcomeEnum.SENDING_RT_FAILED_RESCHEDULING_FAILED.name());

                rtReceiptCosmosService.updateReceiptStatus(ci, iuv, ccp, ReceiptStatusEnum.NOT_SENT);
            }
        } else {

            MDC.put(Constants.MDC_OUTCOME, OutcomeEnum.SENDING_RT_FAILED_MAX_RETRIES.name());

            rtReceiptCosmosService.updateReceiptStatus(ci, iuv, ccp, ReceiptStatusEnum.NOT_SENT);
        }
    }


}
