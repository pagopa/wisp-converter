package it.gov.pagopa.wispconverter.consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@ExtendWith(MockitoExtension.class)
public class PaymentTimeoutConsumerTest {

    private static final String MOCK_CONNECTION_STRING = "Endpoint=sb://mock-servicebus.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=mock-key";

    @Mock
    private ReceiptService receiptService;

    @Mock
    private ServiceBusProcessorClient receiverClient;

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

        // Mock the message body
        ReceiptDto receiptDto = new ReceiptDto();
        ObjectMapper mapper = new ObjectMapper();
        byte[] byteArray = mapper.writeValueAsBytes(receiptDto);
        BinaryData binaryData = BinaryData.fromBytes(byteArray);
        when(message.getBody()).thenReturn(binaryData);

        paymentTimeoutConsumer.processMessage(context);

        verify(receiptService, times(1)).paaInviaRTKo(anyString());
    }

    @Test
    public void testPost() {
        paymentTimeoutConsumer.post();
        assertNotNull(ReflectionTestUtils.getField(paymentTimeoutConsumer, "receiverClient"));
    }
}
