package it.gov.pagopa.wispconverter.servicebus;

import com.azure.messaging.servicebus.ServiceBusMessage;

import java.time.OffsetDateTime;

/*
 * ServiceBusSenderClient is a final class, in order to test and inherit this class,
 * the wrapper interface was created
 */
public interface ServiceBusSenderClientWrapper {
    long scheduleMessage(ServiceBusMessage message, OffsetDateTime scheduledEnqueueTime);
    void cancelScheduledMessage(long sequenceNumber);
}
