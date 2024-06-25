package it.gov.pagopa.wispconverter.servicebus;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.service.ReceiptService;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
public class PaymentTimeoutConsumer extends SBConsumer {

    private final ObjectMapper mapper = new ObjectMapper();
    @Value("${azure.sb.wisp-payment-timeout-queue.connectionString}")
    private String connectionString;
    @Value("${azure.sb.queue.receiptTimer.name}")
    private String queueName;
    @Autowired
    private ReceiptService receiptService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeClient() {
        if (receiverClient != null) {
            log.debug("[Scheduled] Starting PaymentTimeoutConsumer {}", ZonedDateTime.now());
            receiverClient.start();
        }
    }

    @PostConstruct
    public void post() {
        if (StringUtils.isNotBlank(connectionString) && !connectionString.equals("-")) {
            receiverClient = CommonUtility.getServiceBusProcessorClient(connectionString, queueName, this::processMessage, this::processError);
        }
    }

    public void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.debug("Processing message. Session: {}, Sequence #: {}. Contents: {}", message.getMessageId(),
                message.getSequenceNumber(), message.getBody());
        try {
            ReceiptDto receiptDto = mapper.readValue(message.getBody().toStream(), ReceiptDto.class);
            // transform to string list
            String inputPaaInviaRTKo = List.of(receiptDto).toString();
            receiptService.sendKoPaaInviaRtToCreditorInstitution(inputPaaInviaRTKo);
        } catch (IOException e) {
            log.error("Error when read ReceiptDto value from message: '{}'. Body: '{}'",
                    message.getMessageId(), message.getBody());
        }
    }
}
