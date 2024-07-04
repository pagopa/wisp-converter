package it.gov.pagopa.wispconverter.servicebus;

import com.azure.messaging.servicebus.*;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// Service Bus Consumer
@Slf4j
public abstract class SBConsumer {
    protected ServiceBusProcessorClient receiverClient;

    @Autowired
    private ReService reService;

    @PreDestroy
    public void preDestroy() {
        receiverClient.close();
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

    protected void setSessionDataInfoInMDC(String businessProcess) {
        String operationId = UUID.randomUUID().toString();
        MDC.put(Constants.MDC_START_TIME, String.valueOf(System.currentTimeMillis()));
        MDC.put(Constants.MDC_OPERATION_ID, operationId);
        MDC.put(Constants.MDC_REQUEST_ID, operationId);
        MDC.put(Constants.MDC_BUSINESS_PROCESS, businessProcess);
    }

    protected void generateRE(InternalStepStatus status, String otherInfo) {

        ReEventDto reEvent = ReUtil.getREBuilder()
                .status(status)
                .info(otherInfo)
                .build();
        reService.addRe(reEvent);
    }

}
