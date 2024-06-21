package it.gov.pagopa.wispconverter.servicebus;

import com.azure.messaging.servicebus.*;
import gov.telematici.pagamenti.ws.nodoperpa.ppthead.IntestazionePPT;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.IdempotencyStatusEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptTypeEnum;
import it.gov.pagopa.wispconverter.service.*;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.MDCUtil;
import it.gov.pagopa.wispconverter.util.ReUtil;
import it.gov.pagopa.wispconverter.util.ZipUtil;
import jakarta.xml.soap.SOAPMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RTConsumer {

    @Value("${azure.sb.connectionString}")
    private String connectionString;

    @Value("${azure.sb.paaInviaRT.name}")
    private String queueName;

    @Value("${wisp-converter.rt-send.max-retries}")
    private Integer maxRetries;

    @Value("${wisp-converter.rt-send.scheduling-time-in-hours}")
    private Integer schedulingTimeInHours;

    @Autowired
    private RtCosmosService rtCosmosService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private PaaInviaRTSenderService paaInviaRTSenderService;

    private ServiceBusProcessorClient receiverClient;

    private ServiceBusService serviceBusService;

    private ReService reService;

    @Autowired
    private JaxbElementUtil jaxbElementUtil;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeClient() {
        if (receiverClient != null) {
            log.info("[Scheduled] Starting RTConsumer {}", ZonedDateTime.now());
            receiverClient.start();
        }
    }

    @PostConstruct
    public void post() {
        if (StringUtils.isNotBlank(connectionString) && !connectionString.equals("-")) {
            receiverClient = new ServiceBusClientBuilder()
                    .connectionString(connectionString)
                    .processor()
                    .queueName(queueName)
                    .processMessage(this::processMessage)
                    .processError(this::processError)
                    .buildProcessorClient();
        }

    }

    @PreDestroy
    public void preDestroy() {
        receiverClient.close();
    }

    public void processMessage(ServiceBusReceivedMessageContext context) {

        // retrieving content from context of arrived message
        ServiceBusReceivedMessage message = context.getMessage();
        log.info("Processing " + message.getMessageId());

        // extracting the values needed for the search of the receipt persisted in storage
        String compositedIdForReceipt = new String(message.getBody().toBytes());
        String[] idSections = compositedIdForReceipt.split("_");
        String rtInsertionDate = idSections[0];
        String receiptId = idSections[1] + "_" + idSections[2];

        // get RT request entity from database
        RTRequestEntity rtRequestEntity = rtCosmosService.getRTRequestEntity(receiptId, rtInsertionDate);
        String idempotencyKey = rtRequestEntity.getIdempotencyKey();
        ReceiptTypeEnum receiptType = rtRequestEntity.getReceiptType();

        try {

            // before sending the RT to the creditor institution, the idempotency key must be checked in order to not send duplicated receipts
            if (idempotencyService.isIdempotencyKeyProcessable(idempotencyKey, receiptType)) {

                // Lock idempotency key status to avoid concurrency issues
                idempotencyService.lockIdempotencyKey(idempotencyKey, receiptType);

                // If receipt was found, it must be sent to creditor institution, so it try this operation
                log.debug("Sending message {}, retry: {}", compositedIdForReceipt, rtRequestEntity.getRetry());
                resendRTToCreditorInstitution(receiptId, rtRequestEntity, compositedIdForReceipt, idempotencyKey);

                // Unlock idempotency key after a successful operation
                idempotencyService.unlockIdempotencyKey(idempotencyKey, receiptType, IdempotencyStatusEnum.SUCCESS);

            } else {

                // Status was locked due to concurrent execution, so it will be retried at the next execution (but only if it is not completed)
                if (!idempotencyService.isCompleted(idempotencyKey)) {
                    reScheduleReceiptSend(rtRequestEntity, receiptId, compositedIdForReceipt);
                }
            }

        } catch (Exception e) {

            // Generate a new event in RE for store the unsuccessful re-sending of the receipt
            generateREForNotSentRT(e);


            // Unlock idempotency key after a failed operation
            idempotencyService.unlockIdempotencyKey(idempotencyKey, receiptType, IdempotencyStatusEnum.FAILED);
        }
    }

    private void resendRTToCreditorInstitution(String receiptId, RTRequestEntity receipt, String compositedIdForReceipt, String idempotencyKey) {

        try {

            log.debug("Sending receipt [{}]", receiptId);

            // unzip retrieved zipped payload from GZip format
            byte[] unzippedPayload = ZipUtil.unzip(receipt.getPayload().getBytes(StandardCharsets.UTF_8));
            SOAPMessage envelopeElement = jaxbElementUtil.getMessage(unzippedPayload);
            IntestazionePPT header = jaxbElementUtil.getHeader(envelopeElement, IntestazionePPT.class);

            // set MDC session data for RE
            String[] idempotencyKeySections = idempotencyKey.split("_");
            MDCUtil.setSessionDataInfoInMDC(header, idempotencyKeySections[2]);

            String rawPayload = new String(unzippedPayload);
            paaInviaRTSenderService.sendToCreditorInstitution(receipt.getUrl(), rawPayload);
            rtCosmosService.deleteRTRequestEntity(receipt);
            log.info("Sent receipt [{}]", receiptId);

            // generate a new event in RE for store the successful re-sending of the receipt
            generateREForSentRT();

        } catch (AppException e) {

            // generate a new event in RE for store the unsuccessful re-sending of the receipt
            generateREForNotSentRT(e);

            // Rescheduled due to errors caused by faulty communication with creditor institution
            reScheduleReceiptSend(receipt, receiptId, compositedIdForReceipt);

        } catch (IOException e) {

            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_ZIPPED_PAYLOAD);
        }
    }

    private void reScheduleReceiptSend(RTRequestEntity receipt, String receiptId, String compositedIdForReceipt) {

        // because of the not sent receipt, it is necessary to schedule a retry of the sending process for this receipt
        if (receipt.getRetry() < this.maxRetries - 1) {

            try {

                // if required, update the retry count for the retrieved RT
                log.debug("Increasing retry by one and saving receipt with id: [{}]", receiptId);
                receipt.setRetry(receipt.getRetry() + 1);
                rtCosmosService.saveRTRequestEntity(receipt);

                // because of the not sent receipt, it is necessary to schedule a retry of the sending process for this receipt
                serviceBusService.sendMessage(compositedIdForReceipt, schedulingTimeInHours);

                // generate a new event in RE for store the successful scheduling of the RT send
                generateREForSuccessfulReschedulingSentRT();

            } catch (Exception e) {

                // generate a new event in RE for store the unsuccessful scheduling of the RT send
                generateREForFailedReschedulingSentRT(e);
            }
        }
    }

    public void processError(ServiceBusErrorContext context) {

        if (!(context.getException() instanceof ServiceBusException exception)) {
            log.error("Non-ServiceBusException occurred", context.getException());
            return;
        }

        ServiceBusFailureReason reason = exception.getReason();

        if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED || reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND || reason == ServiceBusFailureReason.UNAUTHORIZED) {
            log.error("An unrecoverable error occurred. Stopping processing with reason {}:{}", reason, exception.getMessage());
        } else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
            log.error("Message lock lost for message: %s%n", context.getException());
        } else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {

            try {
                // Choosing an arbitrary amount of time to wait until trying again.
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("Unable to sleep for period of time");
                Thread.currentThread().interrupt();
            }

        } else {
            log.error("Error source {}, reason {}, message: {}", context.getErrorSource(), reason, context.getException().toString());
        }
    }


    private void generateREForSentRT() {

        generateRE(InternalStepStatus.RT_SCHEDULED_SEND_SUCCESS, "Re-scheduled send operation: success.");
    }

    private void generateREForNotSentRT(Throwable e) {

        generateRE(InternalStepStatus.RT_SCHEDULED_SEND_FAILURE, "Re-scheduled send operation: failure. Caused by: " + e.getMessage());
    }

    private void generateREForSuccessfulReschedulingSentRT() {

        generateRE(InternalStepStatus.RT_SEND_RESCHEDULING_SUCCESS, "Trying to re-schedule for next retry: success.");
    }

    private void generateREForFailedReschedulingSentRT(Throwable exception) {

        generateRE(InternalStepStatus.RT_SEND_RESCHEDULING_FAILURE, "Trying to re-schedule for next retry: failure. Caused by: " + exception.getMessage());
    }

    private void generateRE(InternalStepStatus status, String otherInfo) {

        ReEventDto reEvent = ReUtil.getREBuilder()
                .status(status)
                .info(otherInfo)
                .build();
        reService.addRe(reEvent);
    }
}
