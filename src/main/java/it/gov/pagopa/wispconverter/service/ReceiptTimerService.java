package it.gov.pagopa.wispconverter.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import it.gov.pagopa.wispconverter.controller.model.ReceiptTimerRequest;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiptTimerService {

    private static final String CACHING_KEY_TEMPLATE = "wisp_timer_%s";

    @Value("${azure.sb.connectionString}")
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
        log.info("Sending scheduled message {} to the queue: {}", message, queueName);
        // compute time and schedule message for consumer trigger
        OffsetDateTime scheduledExpirationTime = OffsetDateTime.now().plus(message.getExpirationTime(), ChronoUnit.MILLIS);
        Long sequenceNumber = serviceBusSenderClient.scheduleMessage(serviceBusMessage, scheduledExpirationTime);
        log.info("Sent scheduled message {} to the queue: {}", message, queueName);
        // Insert {wisp_timer_<paymentToken>, sequenceNumber} for Duplicate Prevention Logic and for call cancelScheduledMessage(sequenceNumber)
        cacheRepository.insert(sequenceNumberKey, String.valueOf(sequenceNumber), message.getExpirationTime());
        log.info("Cache sequence number {} for payment-token: {}", sequenceNumber, sequenceNumberKey);
    }
}
