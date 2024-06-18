package it.gov.pagopa.wispconverter.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import it.gov.pagopa.wispconverter.controller.model.ReceiptTimerRequest;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiptTimerService {


    @Value("${azure.sb.connectionString}")
    private String connectionString;

    @Value("${azure.sb.queue.receiptTimer.name}")
    private String queueName;

    @Autowired
    private ServiceBusSenderClient serviceBusSenderClient;

    public void sendMessage(ReceiptTimerRequest message) {
        ReceiptDto receiptDto = ReceiptDto.builder()
                .paymentToken(message.getPaymentToken())
                .fiscalCode(message.getFiscalCode())
                .noticeNumber(message.getNoticeNumber())
                .build();
        ServiceBusMessage serviceBusMessage = new ServiceBusMessage(receiptDto.toString());
        log.debug("Sending scheduled message {} to the queue: {}", message, queueName);
        OffsetDateTime scheduledExpirationTime = OffsetDateTime.now().plus(message.getExpirationTime(), ChronoUnit.MILLIS);
        serviceBusSenderClient.scheduleMessage(serviceBusMessage, scheduledExpirationTime);
        log.debug("Sent scheduled message {} to the queue: {}", message, queueName);
    }
}
