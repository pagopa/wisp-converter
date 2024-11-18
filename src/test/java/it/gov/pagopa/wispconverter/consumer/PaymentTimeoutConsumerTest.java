package it.gov.pagopa.wispconverter.consumer;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.service.ReceiptService;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.servicebus.PaymentTimeoutConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentTimeoutConsumerTest {

    private static final String MOCK_CONNECTION_STRING = "Endpoint=sb://mock-servicebus.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=mock-key";

    @Mock
    private ReceiptService receiptService;

    @Mock
    private ServiceBusProcessorClient receiverClient;

    @Mock
    private ReService reService;

    @InjectMocks
    private PaymentTimeoutConsumer paymentTimeoutConsumer;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(paymentTimeoutConsumer, "connectionString", MOCK_CONNECTION_STRING);
        ReflectionTestUtils.setField(paymentTimeoutConsumer, "queueName", "mock-queue-name");
        paymentTimeoutConsumer.post();
    }

    @Test
    public void testInitializeClient() {
        // Set the receiverClient mock
        ReflectionTestUtils.setField(paymentTimeoutConsumer, "receiverClient", receiverClient);

        paymentTimeoutConsumer.initializeClient();

        verify(receiverClient, times(1)).start();
    }

    @Test
    public void testPreDestroy() {
        // Set the receiverClient mock
        ReflectionTestUtils.setField(paymentTimeoutConsumer, "receiverClient", receiverClient);

        paymentTimeoutConsumer.preDestroy();

        verify(receiverClient, times(1)).close();
    }

    @Test
    public void testProcessMessage() throws Exception {
        ServiceBusReceivedMessageContext context = mock(ServiceBusReceivedMessageContext.class);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);

        when(context.getMessage()).thenReturn(message);
        when(message.getMessageId()).thenReturn("messageId");
        when(message.getSequenceNumber()).thenReturn(1L);

        doNothing().when(reService).sendEvent(any(), any(), any(), any());

        // Mock the message body
        ReceiptDto receiptDto = new ReceiptDto();
        ObjectMapper mapper = new ObjectMapper();
        byte[] byteArray = mapper.writeValueAsBytes(receiptDto);
        BinaryData binaryData = BinaryData.fromBytes(byteArray);
        when(message.getBody()).thenReturn(binaryData);

        paymentTimeoutConsumer.processMessage(context);

        verify(receiptService, times(1)).sendKoPaaInviaRtToCreditorInstitution(anyList());
    }

    @Test
    public void testPost() {
        paymentTimeoutConsumer.post();
        assertNotNull(ReflectionTestUtils.getField(paymentTimeoutConsumer, "receiverClient"));
    }

    @Test
    public void testProcessErrorExecution_SERVICE_BUSY() {
        testProcessErrorExecution(ServiceBusFailureReason.SERVICE_BUSY, 1);
    }

    @Test
    public void testProcessErrorExecution_MESSAGING_ENTITY_DISABLED() {
        testProcessErrorExecution(ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED, 1);
    }

    @Test
    public void testProcessErrorExecution_MESSAGING_ENTITY_NOT_FOUND() {
        testProcessErrorExecution(ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND, 1);
    }

    @Test
    public void testProcessErrorExecution_MESSAGE_LOCK_LOST() {
        testProcessErrorExecution(ServiceBusFailureReason.MESSAGE_LOCK_LOST, 2);
    }

    @Test
    public void testProcessErrorExecution_UNAUTHORIZED() {
        testProcessErrorExecution(ServiceBusFailureReason.UNAUTHORIZED, 1);
    }

    private void testProcessErrorExecution(ServiceBusFailureReason serviceBusFailureReason, int wantedNumberOfInvocations) {
        // Create a mock context and exception
        ServiceBusErrorContext context = mock(ServiceBusErrorContext.class);
        ServiceBusException exception = mock(ServiceBusException.class);

        // Mock the context and exception behavior
        when(context.getException()).thenReturn(exception);
        when(exception.getReason()).thenReturn(serviceBusFailureReason);

        // Call the processError method
        paymentTimeoutConsumer.processError(context);

        // Verify that the method executes correctly
        verify(context, times(wantedNumberOfInvocations)).getException();
        verify(exception, times(1)).getReason();
    }
}
