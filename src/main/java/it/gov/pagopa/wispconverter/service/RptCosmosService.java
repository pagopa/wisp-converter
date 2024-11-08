package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RptCosmosService {

    private final ReService reService;

    private final RPTRequestRepository rptRequestRepository;

    @Value("${wisp-converter.re-tracing.internal.rpt-retrieving.enabled}")
    private Boolean isTracingOnREEnabled;

    public RPTRequestEntity getRPTRequestEntity(String sessionId) {
        MDC.put(Constants.MDC_SESSION_ID, sessionId);

        // searching RPT by session identifier: if no element is found throw an exception, in the RE will be saved an exception event of failure
        Optional<RPTRequestEntity> optRPTReqEntity = this.rptRequestRepository.findById(sessionId);

        return optRPTReqEntity.orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.PERSISTENCE_RPT_NOT_FOUND, sessionId));
    }
}
