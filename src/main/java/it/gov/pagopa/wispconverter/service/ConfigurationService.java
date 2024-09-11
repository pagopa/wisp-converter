package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.ConfigurationRepository;
import it.gov.pagopa.wispconverter.repository.model.ConfigurationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigurationService {

    private static final String CREDITOR_INSTITUTION_ID = "cis";

    private static final String STATION_ID = "stations";

    private final ConfigurationRepository configurationRepositoryRepository;

    public String getCreditorInstitutionConfiguration(){
        ConfigurationEntity configurationEntity = configurationRepositoryRepository.findById(CREDITOR_INSTITUTION_ID)
                .orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.CONFIGURATION_NOT_FOUND));
        return configurationEntity.getContent();
    }

    public String getStationConfiguration(){
        ConfigurationEntity configurationEntity = configurationRepositoryRepository.findById(STATION_ID)
                .orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.CONFIGURATION_NOT_FOUND));
        return configurationEntity.getContent();
    }
}
