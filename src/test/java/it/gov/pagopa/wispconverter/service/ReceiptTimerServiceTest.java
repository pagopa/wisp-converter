package it.gov.pagopa.wispconverter.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import it.gov.pagopa.wispconverter.controller.model.ReceiptTimerRequest;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReceiptTimerServiceTest {
    private static final String MOCK_CONNECTION_STRING = "Endpoint=sb://mock-servicebus.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=mock-key";

    @Mock
    private ServiceBusSenderClient serviceBusSenderClient;

    @Mock
    private ServiceBusSenderAsyncClient asyncSender;

    @Mock
    private CacheRepository cacheRepository;

    @InjectMocks
    private ReceiptTimerService receiptTimerService;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(receiptTimerService, "connectionString", MOCK_CONNECTION_STRING);
        ReflectionTestUtils.setField(receiptTimerService, "queueName", "your-queue-name");

        receiptTimerService.post();
    }

    @Test
    public void testSendMessage_duplicateMessage() {
        ReceiptTimerRequest request = new ReceiptTimerRequest();
        request.setPaymentToken("token123");
        request.setFiscalCode("fiscalCode");
        request.setNoticeNumber("noticeNumber");
        request.setExpirationTime(1000L);

        when(cacheRepository.read(any(String.class), eq(String.class))).thenReturn("existingSequenceNumber");

        receiptTimerService.sendMessage(request);

        verify(serviceBusSenderClient, times(0)).scheduleMessage(any(ServiceBusMessage.class), any(OffsetDateTime.class));
        verify(cacheRepository, times(0)).insert(any(String.class), any(String.class), any(Long.class), any(ChronoUnit.class));
    }

    @Test
    public void testSendMessage_newMessage() {
        ReceiptTimerRequest request = new ReceiptTimerRequest();
        request.setPaymentToken("token123");
        request.setFiscalCode("fiscalCode");
        request.setNoticeNumber("noticeNumber");
        request.setExpirationTime(1000L);

        when(cacheRepository.read(any(String.class), eq(String.class))).thenReturn(null);
        when(serviceBusSenderClient.scheduleMessage(any(ServiceBusMessage.class), any(OffsetDateTime.class))).thenReturn(123L);

        receiptTimerService.sendMessage(request);

        verify(serviceBusSenderClient, times(1)).scheduleMessage(any(ServiceBusMessage.class), any(OffsetDateTime.class));
        verify(cacheRepository, times(1)).insert(any(String.class), eq("123"), eq(1000L), eq(ChronoUnit.MILLIS));
    }

    /*
    @Test
    public void testCancelScheduledMessage_callCancelScheduledMessage() {
        List<String> paymentTokens = List.of("token1", "token2");
        String sequenceNumberKey1 = "wisp_timer_token1";
        String sequenceNumberKey2 = "wisp_timer_token2";
        long sequenceNumber1 = 123L;
        long sequenceNumber2 = 456L;

        when(cacheRepository.read(sequenceNumberKey1, String.class)).thenReturn(Long.toString(sequenceNumber1));
        when(cacheRepository.read(sequenceNumberKey2, String.class)).thenReturn(Long.toString(sequenceNumber2));
        //doNothing().when(serviceBusSenderClient).cancelScheduledMessage(anyLong());
        when(asyncSender.cancelScheduledMessage(123L)).thenReturn(Mono.empty());
        when(asyncSender.cancelScheduledMessage(456L)).thenReturn(Mono.empty());

        // Call method under test
        receiptTimerService.cancelScheduledMessage(paymentTokens);

        // Verify interactions
        verify(cacheRepository).delete(sequenceNumberKey1);
        verify(cacheRepository).delete(sequenceNumberKey2);
    }
    */

    @Test
    public void testCancelScheduledMessage_notFound() {
        List<String> paymentTokens = List.of("token1");

        when(cacheRepository.read(any(String.class), eq(String.class))).thenReturn(null);

        receiptTimerService.cancelScheduledMessage(paymentTokens);

        verify(serviceBusSenderClient, times(0)).cancelScheduledMessage(anyLong());
        verify(cacheRepository, times(0)).delete(any(String.class));
    }
}
