package it.gov.pagopa.wispconverter.servicebus;

import com.azure.messaging.servicebus.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

// Service Bus Consumer
@Slf4j
public abstract class SBConsumer {

    protected ServiceBusProcessorClient createServiceBusProcessorClient(String connectionString, String queueName){
        if (StringUtils.isNotBlank(connectionString) && !connectionString.equals("-"))
            return null;
        else {
            return new ServiceBusClientBuilder()
                    .connectionString(connectionString)
                    .processor()
                    .queueName(queueName)
                    .processMessage(this::processMessage)
                    .processError(this::processError)
                    .buildProcessorClient();
        }
    }

    protected void initServiceBusProcessorClient(ServiceBusProcessorClient receiverClient) {
        if(receiverClient != null){
            log.info("[Scheduled] Starting RTConsumer {}", ZonedDateTime.now());
            receiverClient.start();
        }
    }

    public abstract void processMessage(ServiceBusReceivedMessageContext context);

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
                Thread.currentThread().interrupt();
            }
        } else {
            log.error("Error source {}, reason {}, message: {}", context.getErrorSource(),
                    reason, context.getException());
        }
    }

}
