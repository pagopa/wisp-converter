package it.gov.pagopa.wispconverter.servicebus;

import com.azure.messaging.servicebus.*;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.service.ReceiptService;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentTimeoutConsumer {

    @Value("${azure.sb.connectionString}")
    private String connectionString;

    @Value("${azure.sb.queue.receiptTimer.name}")
    private String queueName;

    @Autowired
    private ReceiptService receiptService;

    private final ObjectMapper mapper = new ObjectMapper();

    private ServiceBusProcessorClient receiverClient;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeClient() {
        if(receiverClient!=null){
            log.info("[Scheduled] Starting PaymentTimeoutConsumer {}", ZonedDateTime.now());
            receiverClient.start();
        }
    }

    @PostConstruct
    public void post(){
    if (StringUtils.isNotBlank(connectionString) && !connectionString.equals("-")) {
      receiverClient =
          new ServiceBusClientBuilder()
              .connectionString(connectionString)
              .processor()
              .queueName(queueName)
              .processMessage(this::processMessage)
              .processError(this::processError)
              .buildProcessorClient();
        }
    }

    @PreDestroy
    public void preDestroy(){
        receiverClient.close();
    }

    public void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.info("Processing message. Session: {}, Sequence #: {}. Contents: {}", message.getMessageId(),
                message.getSequenceNumber(), message.getBody());
        try {
            ReceiptDto receiptDto = mapper.readValue(message.getBody().toStream(), ReceiptDto.class);
            // transform to string list
            String inputPaaInviaRTKo = List.of(receiptDto).toString();
            receiptService.paaInviaRTKo(inputPaaInviaRTKo);
        } catch (IOException e) {
            log.error("Error when read ReceiptDto value from message: '{}'. Body: '{}'",
                    message.getMessageId(), message.getBody());
        }
    }

    public void processError(ServiceBusErrorContext context) {
        log.error("Error when receiving messages from namespace: '{}'. Entity: '{}'",
                context.getFullyQualifiedNamespace(), context.getEntityPath());

        if (!(context.getException() instanceof ServiceBusException exception)) {
            log.error("Non-ServiceBusException occurred: {}", context.getException().getMessage());
            return;
        }

        ServiceBusFailureReason reason = exception.getReason();

        if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED
                || reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND
                || reason == ServiceBusFailureReason.UNAUTHORIZED) {
            log.error("An unrecoverable error occurred. Stopping processing with reason {}: {}",
                    reason, exception.getMessage());
        } else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
            log.error("Message lock lost for message: {}", context.getException().getMessage());
        } else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
            try {
                // Choosing an arbitrary amount of time to wait until trying again.
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.debug("Unable to sleep for period of time");
            }
        } else {
            log.error("Error source {}, reason {}, message: {}", context.getErrorSource(),
                    reason, context.getException());
        }
    }
}
