package it.gov.pagopa.wispconverter.servicebus;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusFailureReason;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

// Service Bus Consumer
@Slf4j
public abstract class SBConsumer {

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
