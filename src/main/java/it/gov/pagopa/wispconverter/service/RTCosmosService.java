package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.repository.RTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RTCosmosService {

    private final RTRequestRepository rtRequestRepository;

    @Transactional
    public void saveRTRequestEntity(RTRequestEntity rtRequestEntity) {
        rtRequestRepository.save(rtRequestEntity);
    }
}
