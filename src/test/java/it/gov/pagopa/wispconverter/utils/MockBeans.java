package it.gov.pagopa.wispconverter.utils;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import it.gov.pagopa.wispconverter.repository.*;
import it.gov.pagopa.wispconverter.service.ECommerceHangTimerService;
import it.gov.pagopa.wispconverter.service.ReceiptTimerService;
import it.gov.pagopa.wispconverter.service.ServiceBusService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MockBeans {

    @Bean
    @Primary
    ConfigurationRepository configurationRepository() {
        return Mockito.mock(ConfigurationRepository.class);
    }

    @Bean
    @Primary
    RPTRequestRepository rptRequestRepository() {
        return Mockito.mock(RPTRequestRepository.class);
    }

    @Bean
    @Primary
    RTRetryRepository rtRetryRepository() {
        return Mockito.mock(RTRetryRepository.class);
    }

    @Bean
    @Primary
    RTRepository rtRepository() {
        return Mockito.mock(RTRepository.class);
    }

    @Bean
    @Primary
    ReEventRepository reEventRepository() {
        return Mockito.mock(ReEventRepository.class);
    }

    @Bean
    @Primary
    IdempotencyKeyRepository idempotencyKeyRepository() {
        return Mockito.mock(IdempotencyKeyRepository.class);
    }


    @Bean
    @Primary
    ServiceBusService serviceBusService() {
        return Mockito.mock(ServiceBusService.class);
    }

    @Bean
    @Primary
    ServiceBusSenderClient serviceBusSenderClient() {
        return Mockito.mock(ServiceBusSenderClient.class);
    }

    @Bean
    @Primary
    ReceiptTimerService receiptTimerService() {
        return Mockito.mock(ReceiptTimerService.class);
    }

    @Bean
    @Primary
    ECommerceHangTimerService eCommerceHangTimerService() {
        return Mockito.mock(ECommerceHangTimerService.class);
    }
}
