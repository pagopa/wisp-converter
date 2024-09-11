package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.controller.model.ConfigurationModel;
import it.gov.pagopa.wispconverter.repository.ConfigurationRepository;
import it.gov.pagopa.wispconverter.repository.model.ConfigurationEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles(profiles = "test")
@SpringBootTest
class ConfigurationServiceTest {

    @MockBean
    ConfigurationRepository configurationRepository;

    @Autowired
    @InjectMocks
    ConfigurationService configurationService;


    @Captor
    ArgumentCaptor<ConfigurationEntity> argumentCaptor;




    @Test
    void createCreditorInstitutionsConfiguration() {
        configurationService.createCreditorInstitutionsConfiguration(ConfigurationModel.builder().build());
        verify(configurationRepository, times(1)).save(argumentCaptor.capture());
        assertEquals("cis", argumentCaptor.getValue().getId());

    }

    @Test
    void createStationsConfiguration() {
        configurationService.createStationsConfiguration(ConfigurationModel.builder().build());
        verify(configurationRepository, times(1)).save(argumentCaptor.capture());
        assertEquals("stations", argumentCaptor.getValue().getId());
    }
}