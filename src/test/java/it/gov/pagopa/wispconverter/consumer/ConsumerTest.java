package it.gov.pagopa.wispconverter.consumer;

import com.azure.core.amqp.exception.AmqpErrorCondition;
import com.azure.core.amqp.exception.AmqpException;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.*;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptTypeEnum;
import it.gov.pagopa.wispconverter.service.*;
import it.gov.pagopa.wispconverter.servicebus.RTConsumer;
import it.gov.pagopa.wispconverter.util.AppBase64Util;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.ZipUtil;
import it.gov.pagopa.wispconverter.utils.TestUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ConsumerTest {

    @SneakyThrows
    private RTRequestEntity getStoredReceipt(int retries, String rawtype, String url) {
        ReceiptTypeEnum type = ReceiptTypeEnum.valueOf(rawtype.toUpperCase());
        String payload = TestUtils.loadFileContent("/requests/paaInviaRT.xml");
        return RTRequestEntity.builder()
                .payload(AppBase64Util.base64Encode(ZipUtil.zip(payload)))
                .retry(retries)
                .receiptType(type)
                .url(url)
                .idempotencyKey("idpa_uuid_nav")
                .build();
    }

    @ParameterizedTest
    @CsvSource(value = {"ok", "ko"})
    void sendToPa_sent(String receiptType) {

        ServiceBusService serviceBusService = mock(ServiceBusService.class);

        ServiceBusReceivedMessageContext messageContext = mock(ServiceBusReceivedMessageContext.class);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(messageContext.getMessage()).thenReturn(message);
        BinaryData bindata = mock(BinaryData.class);
        when(message.getBody()).thenReturn(bindata);
        when(message.getSubject()).thenReturn("mystation");
        when(bindata.toBytes()).thenReturn("aaaaa_bbbbb_ccccc".getBytes(StandardCharsets.UTF_8));


        RtCosmosService rtCosmosService = mock(RtCosmosService.class);
        when(rtCosmosService.getRTRequestEntity(any(), any())).thenReturn(getStoredReceipt(0, receiptType, "http://endpoint:443"));

        ReService reService = mock(ReService.class);
        doNothing().when(reService).addRe(any());

        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        when(idempotencyService.isIdempotencyKeyProcessable(any(), any())).thenReturn(true);

        JaxbElementUtil jaxbElementUtil = new JaxbElementUtil();

        RTConsumer rtConsumer = new RTConsumer();
        ConfigCacheService ccs = mock(ConfigCacheService.class);
        when(ccs.getConfigData()).thenReturn(TestUtils.configData("mystation"));

        PaaInviaRTSenderService paaInviaRTSenderService = mock(PaaInviaRTSenderService.class);

        ReflectionTestUtils.setField(rtConsumer, "rtCosmosService", rtCosmosService);
        ReflectionTestUtils.setField(rtConsumer, "paaInviaRTSenderService", paaInviaRTSenderService);
        ReflectionTestUtils.setField(rtConsumer, "serviceBusService", serviceBusService);
        ReflectionTestUtils.setField(rtConsumer, "reService", reService);
        ReflectionTestUtils.setField(rtConsumer, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(rtConsumer, "jaxbElementUtil", jaxbElementUtil);
        ReflectionTestUtils.setField(rtConsumer, "maxRetries", 48);

        rtConsumer.processMessage(messageContext);

        verify(idempotencyService, times(1)).lockIdempotencyKey(any(), any());
        verify(rtCosmosService, times(0)).saveRTRequestEntity(any());
        verify(rtCosmosService, times(1)).deleteRTRequestEntity(any());
        verify(serviceBusService, times(0)).sendMessage(any(), any());
        verify(idempotencyService, times(1)).unlockIdempotencyKey(any(), any(), any());

    }

    @ParameterizedTest
    @CsvSource(value = {"ok", "ko"})
    void sendToPa_onError_reschedulable(String receiptType) {

        ServiceBusService serviceBusService = mock(ServiceBusService.class);

        ServiceBusReceivedMessageContext messageContext = mock(ServiceBusReceivedMessageContext.class);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(messageContext.getMessage()).thenReturn(message);
        BinaryData bindata = mock(BinaryData.class);
        when(message.getBody()).thenReturn(bindata);
        when(message.getSubject()).thenReturn("mystation");
        when(bindata.toBytes()).thenReturn("aaaaa_bbbbb_ccccc".getBytes(StandardCharsets.UTF_8));


        RtCosmosService rtCosmosService = mock(RtCosmosService.class);
        when(rtCosmosService.getRTRequestEntity(any(), any())).thenReturn(getStoredReceipt(0, receiptType, "http://endpoint:443"));

        ReService reService = mock(ReService.class);
        doNothing().when(reService).addRe(any());

        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        when(idempotencyService.isIdempotencyKeyProcessable(any(), any())).thenReturn(true);

        JaxbElementUtil jaxbElementUtil = new JaxbElementUtil();

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
        ReflectionTestUtils.setField(rtConsumer, "jaxbElementUtil", jaxbElementUtil);
        ReflectionTestUtils.setField(rtConsumer, "maxRetries", 48);

        rtConsumer.processMessage(messageContext);

        verify(idempotencyService, times(1)).lockIdempotencyKey(any(), any());
        verify(rtCosmosService, times(1)).saveRTRequestEntity(any());
        verify(rtCosmosService, times(0)).deleteRTRequestEntity(any());
        verify(serviceBusService, times(1)).sendMessage(any(), any());
        verify(idempotencyService, times(1)).unlockIdempotencyKey(any(), any(), any());

    }

    @ParameterizedTest
    @CsvSource(value = {"ok", "ko"})
    void sendToPa_onError_notReschedulable(String receiptType) {

        ServiceBusService serviceBusService = mock(ServiceBusService.class);

        ServiceBusReceivedMessageContext messageContext = mock(ServiceBusReceivedMessageContext.class);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(messageContext.getMessage()).thenReturn(message);
        BinaryData bindata = mock(BinaryData.class);
        when(message.getBody()).thenReturn(bindata);
        when(message.getSubject()).thenReturn("mystation");
        when(bindata.toBytes()).thenReturn("aaaaa_bbbbb_ccccc".getBytes(StandardCharsets.UTF_8));


        RtCosmosService rtCosmosService = mock(RtCosmosService.class);
        when(rtCosmosService.getRTRequestEntity(any(), any())).thenReturn(getStoredReceipt(48, receiptType, "http://endpoint:443"));

        ReService reService = mock(ReService.class);
        doNothing().when(reService).addRe(any());

        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        when(idempotencyService.isIdempotencyKeyProcessable(any(), any())).thenReturn(true);

        JaxbElementUtil jaxbElementUtil = new JaxbElementUtil();

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
        ReflectionTestUtils.setField(rtConsumer, "jaxbElementUtil", jaxbElementUtil);
        ReflectionTestUtils.setField(rtConsumer, "maxRetries", 48);

        rtConsumer.processMessage(messageContext);

        verify(idempotencyService, times(1)).lockIdempotencyKey(any(), any());
        verify(rtCosmosService, times(0)).saveRTRequestEntity(any());
        verify(rtCosmosService, times(0)).deleteRTRequestEntity(any());
        verify(serviceBusService, times(0)).sendMessage(any(), any());
        verify(idempotencyService, times(1)).unlockIdempotencyKey(any(), any(), any());

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
