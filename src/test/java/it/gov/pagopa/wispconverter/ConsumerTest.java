package it.gov.pagopa.wispconverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

import com.azure.core.amqp.exception.AmqpErrorCondition;
import com.azure.core.amqp.exception.AmqpException;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.*;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import it.gov.pagopa.wispconverter.service.PaaInviaRTService;
import it.gov.pagopa.wispconverter.servicebus.RTConsumer;
import it.gov.pagopa.wispconverter.utils.TestUtils;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ConsumerTest {

    @Test
    void ok() throws Exception {

        ServiceBusReceivedMessageContext messageContext = mock(ServiceBusReceivedMessageContext.class);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(messageContext.getMessage()).thenReturn(message);
        BinaryData bindata = mock(BinaryData.class);
        when(message.getBody()).thenReturn(bindata);
        when(message.getSubject()).thenReturn("mystation");
        when(bindata.toBytes()).thenReturn("aaaaa_bbbbb_ccccc".getBytes(StandardCharsets.UTF_8));

        RTRequestRepository rtRequestRepository = mock(RTRequestRepository.class);
        when(rtRequestRepository.findById(any(),any())).thenReturn(Optional.of(RTRequestEntity.builder().retry(0).build()));

        RTConsumer rtConsumer = new RTConsumer();
        ReflectionTestUtils.setField(rtConsumer, "rtRequestRepository", rtRequestRepository);
        ConfigCacheService ccs = mock(ConfigCacheService.class);
        when(ccs.getConfigData()).thenReturn(TestUtils.configData("mystation"));
        ReflectionTestUtils.setField(rtConsumer, "configCacheService", ccs);

        PaaInviaRTService paaInviaRTService = mock(PaaInviaRTService.class);
        ReflectionTestUtils.setField(rtConsumer, "paaInviaRTService", paaInviaRTService);

        rtConsumer.processMessage(messageContext);

        verify(rtRequestRepository,times(1)).delete(any());

    }

    @Test
    void moreThanMax() throws Exception {

        ServiceBusReceivedMessageContext messageContext = mock(ServiceBusReceivedMessageContext.class);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(messageContext.getMessage()).thenReturn(message);
        BinaryData bindata = mock(BinaryData.class);
        when(message.getBody()).thenReturn(bindata);
        when(message.getSubject()).thenReturn("mystation");
        when(bindata.toBytes()).thenReturn("aaaaa_bbbbb_ccccc".getBytes(StandardCharsets.UTF_8));

        RTRequestRepository rtRequestRepository = mock(RTRequestRepository.class);
        when(rtRequestRepository.findById(any(),any())).thenReturn(Optional.of(RTRequestEntity.builder().retry(48).build()));

        RTConsumer rtConsumer = new RTConsumer();
        ReflectionTestUtils.setField(rtConsumer, "rtRequestRepository", rtRequestRepository);
        ConfigCacheService ccs = mock(ConfigCacheService.class);
        when(ccs.getConfigData()).thenReturn(TestUtils.configData("mystation"));
        ReflectionTestUtils.setField(rtConsumer, "configCacheService", ccs);

        PaaInviaRTService paaInviaRTService = mock(PaaInviaRTService.class);
        ReflectionTestUtils.setField(rtConsumer, "paaInviaRTService", paaInviaRTService);

        rtConsumer.processMessage(messageContext);

        verify(rtRequestRepository,times(0)).delete(any());
        verify(rtRequestRepository,times(0)).save(any());

    }

    @Test
    void koSendToPa() throws Exception {

        ServiceBusSenderClient serviceBusSenderClient = mock(ServiceBusSenderClient.class);

        ServiceBusReceivedMessageContext messageContext = mock(ServiceBusReceivedMessageContext.class);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(messageContext.getMessage()).thenReturn(message);
        BinaryData bindata = mock(BinaryData.class);
        when(message.getBody()).thenReturn(bindata);
        when(message.getSubject()).thenReturn("mystation");
        when(bindata.toBytes()).thenReturn("aaaaa_bbbbb_ccccc".getBytes(StandardCharsets.UTF_8));

        RTRequestEntity receipt = RTRequestEntity.builder().retry(0).build();
        RTRequestRepository rtRequestRepository = mock(RTRequestRepository.class);
        when(rtRequestRepository.findById(any(),any())).thenReturn(Optional.of(receipt));

        RTConsumer rtConsumer = new RTConsumer();
        ConfigCacheService ccs = mock(ConfigCacheService.class);
        when(ccs.getConfigData()).thenReturn(TestUtils.configData("mystation"));

        PaaInviaRTService paaInviaRTService = mock(PaaInviaRTService.class);
        doThrow(new AppException(AppErrorCodeMessageEnum.PARSING_GENERIC_ERROR)).when(paaInviaRTService).send(any(),any());

        ReflectionTestUtils.setField(rtConsumer, "rtRequestRepository", rtRequestRepository);
        ReflectionTestUtils.setField(rtConsumer, "configCacheService", ccs);
        ReflectionTestUtils.setField(rtConsumer, "paaInviaRTService", paaInviaRTService);
        ReflectionTestUtils.setField(rtConsumer, "serviceBusSenderClient", serviceBusSenderClient);

        rtConsumer.processMessage(messageContext);

        assertEquals(receipt.getRetry(),1);
        verify(rtRequestRepository,times(1)).save(receipt);
        verify(serviceBusSenderClient,times(1)).sendMessage(any());

    }
    @Test
    public void testprocesserror(){
        ServiceBusErrorContext serviceBusErrorContext = mock(ServiceBusErrorContext.class);
    when(serviceBusErrorContext.getException())
        .thenReturn(new ServiceBusException(new RuntimeException(),ServiceBusErrorSource.UNKNOWN));
        when(serviceBusErrorContext.getErrorSource()).thenReturn(ServiceBusErrorSource.COMPLETE);
        new RTConsumer().processError(serviceBusErrorContext);
        assertTrue(true);
    }
    @Test
    public void testprocesserror2(){
        ServiceBusErrorContext serviceBusErrorContext = mock(ServiceBusErrorContext.class);
        when(serviceBusErrorContext.getException())
                .thenReturn(new ServiceBusException(new AmqpException(true, AmqpErrorCondition.MESSAGE_LOCK_LOST,"",null),ServiceBusErrorSource.UNKNOWN));
        when(serviceBusErrorContext.getErrorSource()).thenReturn(ServiceBusErrorSource.COMPLETE);
        new RTConsumer().processError(serviceBusErrorContext);
        assertTrue(true);
    }
    @Test
    public void testprocesserror3(){
        ServiceBusErrorContext serviceBusErrorContext = mock(ServiceBusErrorContext.class);
        when(serviceBusErrorContext.getException())
                .thenReturn(new ServiceBusException(new AmqpException(true, AmqpErrorCondition.UNAUTHORIZED_ACCESS,"",null),ServiceBusErrorSource.UNKNOWN));
        when(serviceBusErrorContext.getErrorSource()).thenReturn(ServiceBusErrorSource.COMPLETE);
        new RTConsumer().processError(serviceBusErrorContext);
        assertTrue(true);
    }
    @Test
    public void testprocesserror4(){
        ServiceBusErrorContext serviceBusErrorContext = mock(ServiceBusErrorContext.class);
        when(serviceBusErrorContext.getException())
                .thenReturn(new ServiceBusException(new AmqpException(true, AmqpErrorCondition.SERVER_BUSY_ERROR,"",null),ServiceBusErrorSource.UNKNOWN));
        when(serviceBusErrorContext.getErrorSource()).thenReturn(ServiceBusErrorSource.COMPLETE);
        new RTConsumer().processError(serviceBusErrorContext);
        assertTrue(true);
    }
    @Test
    public void testprocesserror5(){
        ServiceBusErrorContext serviceBusErrorContext = mock(ServiceBusErrorContext.class);
    when(serviceBusErrorContext.getException())
        .thenReturn(
            new RuntimeException());
        when(serviceBusErrorContext.getErrorSource()).thenReturn(ServiceBusErrorSource.COMPLETE);
        new RTConsumer().processError(serviceBusErrorContext);
        assertTrue(true);
    }
     
}
