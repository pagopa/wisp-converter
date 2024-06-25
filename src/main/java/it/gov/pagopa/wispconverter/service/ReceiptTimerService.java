package it.gov.pagopa.wispconverter.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import it.gov.pagopa.wispconverter.controller.model.ReceiptTimerRequest;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiptTimerService {

    public static final String CACHING_KEY_TEMPLATE = "wisp_timer_%s";

    @Value("${azure.sb.wisp-payment-timeout-queue.connectionString}")
    private String connectionString;

    @Value("${azure.sb.queue.receiptTimer.name}")
    private String queueName;

    private ServiceBusSenderClient serviceBusSenderClient;

    private final CacheRepository cacheRepository;

    @PostConstruct
    public void post() {
        serviceBusSenderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();
    }

    public void sendMessage(ReceiptTimerRequest message) {
        // Duplicate Prevention Logic
        String sequenceNumberKey = String.format(CACHING_KEY_TEMPLATE, message.getPaymentToken());
        String value = cacheRepository.read(sequenceNumberKey, String.class);
        if(value != null) return; // already exists
        // Create Message with paaInviaRT- receipt generation info
        ReceiptDto receiptDto = ReceiptDto.builder()
                .paymentToken(message.getPaymentToken())
                .fiscalCode(message.getFiscalCode())
                .noticeNumber(message.getNoticeNumber())
                .build();
        ServiceBusMessage serviceBusMessage = new ServiceBusMessage(receiptDto.toString());
        log.debug("Sending scheduled message {} to the queue: {}", message, queueName);
        // compute time and schedule message for consumer trigger
        OffsetDateTime scheduledExpirationTime = OffsetDateTime.now().plus(message.getExpirationTime(), ChronoUnit.MILLIS);
        Long sequenceNumber = serviceBusSenderClient.scheduleMessage(serviceBusMessage, scheduledExpirationTime);
        log.info("Sent scheduled message_base64 {} to the queue: {}", LogUtils.encodeToBase64(message.toString()), queueName);
        // insert {wisp_timer_<paymentToken>, sequenceNumber} for Duplicate Prevention Logic and for call cancelScheduledMessage(sequenceNumber)
        cacheRepository.insert(sequenceNumberKey, String.valueOf(sequenceNumber), message.getExpirationTime(), ChronoUnit.MILLIS);
        log.debug("Cache sequence number {} for payment-token: {}", sequenceNumber, sequenceNumberKey);
    }

    public void cancelScheduledMessage(List<String> paymentTokens) {
        paymentTokens.forEach(this::cancelScheduledMessage);
    }

    private void cancelScheduledMessage(String paymentToken) {
        log.debug("Cancel scheduled message for payment-token {}", paymentToken);
        String sequenceNumberKey = String.format(CACHING_KEY_TEMPLATE, paymentToken);
        String sequenceNumberString = cacheRepository.read(sequenceNumberKey, String.class);
        // the message related to payment-token has either already been deleted or it does not exist:
        // without sequenceNumber is not possible to delete from serviceBus -> return
        if(sequenceNumberString == null) return;
        // cancel scheduled message
        if(this.callCancelScheduledMessage(sequenceNumberString)) {
            log.info("Canceled scheduled message for payment-token_base64 {}", LogUtils.encodeToBase64(paymentToken));
            cacheRepository.delete(sequenceNumberKey);
            log.debug("Deleted sequence number {} for payment-token: {} from cache", sequenceNumberString, sequenceNumberKey);
        }
    }

    public boolean callCancelScheduledMessage(String sequenceNumberString) {
        long sequenceNumber = Long.parseLong(sequenceNumberString);
        try {
            serviceBusSenderClient.cancelScheduledMessage(sequenceNumber);
            return true;
        } catch (Exception exception) {
            throw new AppException(AppErrorCodeMessageEnum.PERSISTENCE_SERVICE_BUS_CANCEL_ERROR, exception.getMessage());
        }
    }
}
