package it.gov.pagopa.wispconverter.utils;

import it.gov.pagopa.wispconverter.repository.*;
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
}
