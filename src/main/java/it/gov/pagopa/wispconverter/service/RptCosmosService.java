package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RptCosmosService {

    private final RPTRequestRepository rptRequestRepository;

    public RPTRequestEntity getRPTRequestEntity(String sessionId) {
        Optional<RPTRequestEntity> optRPTReqEntity = this.rptRequestRepository.findById(sessionId);
        return optRPTReqEntity.orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.PERSISTENCE_RPT_NOT_FOUND, sessionId));
        // TODO RE
    }
}
