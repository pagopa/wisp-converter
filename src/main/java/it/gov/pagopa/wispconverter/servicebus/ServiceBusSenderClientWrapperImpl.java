package it.gov.pagopa.wispconverter.servicebus;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

import java.time.OffsetDateTime;

/*
 * ServiceBusSenderClient is a final class in order to be able to test and inherit such a class,
 * the wrapper was implemented
 */
public class ServiceBusSenderClientWrapperImpl implements ServiceBusSenderClientWrapper {
    private final ServiceBusSenderClient serviceBusSenderClient;

    public ServiceBusSenderClientWrapperImpl(ServiceBusSenderClient serviceBusSenderClient) {
        this.serviceBusSenderClient = serviceBusSenderClient;
    }

    @Override
    public long scheduleMessage(ServiceBusMessage message, OffsetDateTime scheduledEnqueueTime) {
        return serviceBusSenderClient.scheduleMessage(message, scheduledEnqueueTime);
    }

    @Override
    public void cancelScheduledMessage(long sequenceNumber) {
        serviceBusSenderClient.cancelScheduledMessage(sequenceNumber);
    }
}
