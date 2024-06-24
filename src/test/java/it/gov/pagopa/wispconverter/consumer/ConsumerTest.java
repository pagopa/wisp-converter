package it.gov.pagopa.wispconverter.consumer;

import com.azure.core.amqp.exception.AmqpErrorCondition;
import com.azure.core.amqp.exception.AmqpException;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.*;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import it.gov.pagopa.wispconverter.service.*;
import it.gov.pagopa.wispconverter.servicebus.RTConsumer;
import it.gov.pagopa.wispconverter.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ConsumerTest {

    @Test
    void ok() {

        ServiceBusReceivedMessageContext messageContext = mock(ServiceBusReceivedMessageContext.class);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(messageContext.getMessage()).thenReturn(message);
        BinaryData bindata = mock(BinaryData.class);
        when(message.getBody()).thenReturn(bindata);
        when(message.getSubject()).thenReturn("mystation");
        when(bindata.toBytes()).thenReturn("aaaaa_bbbbb_ccccc".getBytes(StandardCharsets.UTF_8));

        RTRequestEntity receipt = RTRequestEntity.builder().retry(0).build();
        RtCosmosService rtCosmosService = mock(RtCosmosService.class);
        when(rtCosmosService.getRTRequestEntity(any(), any())).thenReturn(receipt);

        RTConsumer rtConsumer = new RTConsumer();
        ReflectionTestUtils.setField(rtConsumer, "rtCosmosService", rtCosmosService);

        PaaInviaRTSenderService paaInviaRTSenderService = mock(PaaInviaRTSenderService.class);
        ReflectionTestUtils.setField(rtConsumer, "paaInviaRTService", paaInviaRTSenderService);

        rtConsumer.processMessage(messageContext);

        verify(rtCosmosService, times(1)).deleteRTRequestEntity(any());

    }

    @Test
    void moreThanMax() {

        ServiceBusReceivedMessageContext messageContext = mock(ServiceBusReceivedMessageContext.class);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(messageContext.getMessage()).thenReturn(message);
        BinaryData bindata = mock(BinaryData.class);
        when(message.getBody()).thenReturn(bindata);
        when(message.getSubject()).thenReturn("mystation");
        when(bindata.toBytes()).thenReturn("aaaaa_bbbbb_ccccc".getBytes(StandardCharsets.UTF_8));

        RTRequestEntity receipt = RTRequestEntity.builder().retry(48).build();
        RtCosmosService rtCosmosService = mock(RtCosmosService.class);
        when(rtCosmosService.getRTRequestEntity(any(), any())).thenReturn(receipt);

        RTConsumer rtConsumer = new RTConsumer();
        ReflectionTestUtils.setField(rtConsumer, "rtCosmosService", rtCosmosService);

        PaaInviaRTSenderService paaInviaRTSenderService = mock(PaaInviaRTSenderService.class);
        ReflectionTestUtils.setField(rtConsumer, "paaInviaRTService", paaInviaRTSenderService);

        rtConsumer.processMessage(messageContext);

        verify(rtCosmosService, times(0)).deleteRTRequestEntity(any());
        verify(rtCosmosService, times(0)).saveRTRequestEntity(any());

    }

    @Test
    void koSendToPa() {

        ServiceBusService serviceBusService = mock(ServiceBusService.class);

        ServiceBusReceivedMessageContext messageContext = mock(ServiceBusReceivedMessageContext.class);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(messageContext.getMessage()).thenReturn(message);
        BinaryData bindata = mock(BinaryData.class);
        when(message.getBody()).thenReturn(bindata);
        when(message.getSubject()).thenReturn("mystation");
        when(bindata.toBytes()).thenReturn("aaaaa_bbbbb_ccccc".getBytes(StandardCharsets.UTF_8));

        RTRequestEntity receipt = RTRequestEntity.builder().retry(0).idempotencyKey("idempotencykey").build();
        RtCosmosService rtCosmosService = mock(RtCosmosService.class);
        when(rtCosmosService.getRTRequestEntity(any(), any())).thenReturn(receipt);

        ReService reService = mock(ReService.class);
        doNothing().when(reService).addRe(any());

        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        when(idempotencyService.isIdempotencyKeyProcessable(any(), any())).thenReturn(true);

        RTConsumer rtConsumer = new RTConsumer();
        ConfigCacheService ccs = mock(ConfigCacheService.class);
        when(ccs.getConfigData()).thenReturn(TestUtils.configData("mystation"));

        PaaInviaRTSenderService paaInviaRTSenderService = mock(PaaInviaRTSenderService.class);
        doThrow(new AppException(AppErrorCodeMessageEnum.PARSING_GENERIC_ERROR)).when(paaInviaRTSenderService).sendToCreditorInstitution(any(), any());

        ReflectionTestUtils.setField(rtConsumer, "rtCosmosService", rtCosmosService);
        ReflectionTestUtils.setField(rtConsumer, "paaInviaRTSenderService", paaInviaRTSenderService);
        ReflectionTestUtils.setField(rtConsumer, "serviceBusService", serviceBusService);
        ReflectionTestUtils.setField(rtConsumer, "reService", reService);
        ReflectionTestUtils.setField(rtConsumer, "idempotencyService", idempotencyService);

        rtConsumer.processMessage(messageContext);

        verify(rtCosmosService, times(1)).saveRTRequestEntity(receipt);
        verify(serviceBusService, times(1)).sendMessage(any(), any());

    }

    @Test
    void testprocesserror() {
        ServiceBusErrorContext serviceBusErrorContext = mock(ServiceBusErrorContext.class);
        when(serviceBusErrorContext.getException())
                .thenReturn(new ServiceBusException(new RuntimeException(), ServiceBusErrorSource.UNKNOWN));
        when(serviceBusErrorContext.getErrorSource()).thenReturn(ServiceBusErrorSource.COMPLETE);
        new RTConsumer().processError(serviceBusErrorContext);
        assertTrue(true);
    }

    @Test
    void testprocesserror2() {
        ServiceBusErrorContext serviceBusErrorContext = mock(ServiceBusErrorContext.class);
        when(serviceBusErrorContext.getException())
                .thenReturn(new ServiceBusException(new AmqpException(true, AmqpErrorCondition.MESSAGE_LOCK_LOST, "", null), ServiceBusErrorSource.UNKNOWN));
        when(serviceBusErrorContext.getErrorSource()).thenReturn(ServiceBusErrorSource.COMPLETE);
        new RTConsumer().processError(serviceBusErrorContext);
        assertTrue(true);
    }

    @Test
    void testprocesserror3() {
        ServiceBusErrorContext serviceBusErrorContext = mock(ServiceBusErrorContext.class);
        when(serviceBusErrorContext.getException())
                .thenReturn(new ServiceBusException(new AmqpException(true, AmqpErrorCondition.UNAUTHORIZED_ACCESS, "", null), ServiceBusErrorSource.UNKNOWN));
        when(serviceBusErrorContext.getErrorSource()).thenReturn(ServiceBusErrorSource.COMPLETE);
        new RTConsumer().processError(serviceBusErrorContext);
        assertTrue(true);
    }

    @Test
    void testprocesserror4() {
        ServiceBusErrorContext serviceBusErrorContext = mock(ServiceBusErrorContext.class);
        when(serviceBusErrorContext.getException())
                .thenReturn(new ServiceBusException(new AmqpException(true, AmqpErrorCondition.SERVER_BUSY_ERROR, "", null), ServiceBusErrorSource.UNKNOWN));
        when(serviceBusErrorContext.getErrorSource()).thenReturn(ServiceBusErrorSource.COMPLETE);
        new RTConsumer().processError(serviceBusErrorContext);
        assertTrue(true);
    }

    @Test
    void testprocesserror5() {
        ServiceBusErrorContext serviceBusErrorContext = mock(ServiceBusErrorContext.class);
        when(serviceBusErrorContext.getException())
                .thenReturn(
                        new RuntimeException());
        when(serviceBusErrorContext.getErrorSource()).thenReturn(ServiceBusErrorSource.COMPLETE);
        new RTConsumer().processError(serviceBusErrorContext);
        assertTrue(true);
    }

}
