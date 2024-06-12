package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static it.gov.pagopa.wispconverter.util.Constants.NODO_DEI_PAGAMENTI_SPC;

@Service
@Slf4j
@RequiredArgsConstructor
public class RptCosmosService {

    private final ReService reService;

    private final RPTRequestRepository rptRequestRepository;

    public RPTRequestEntity getRPTRequestEntity(String sessionId) {

        // searching RPT by session identifier
        Optional<RPTRequestEntity> optRPTReqEntity = this.rptRequestRepository.findById(sessionId);
        RPTRequestEntity rptRequestEntity = optRPTReqEntity.orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.PERSISTENCE_RPT_NOT_FOUND, sessionId));

        // generate and save RE event internal for change status
        generateRE(rptRequestEntity.getPayload());

        return rptRequestEntity;
    }

    @Transactional
    public void saveRPTRequestEntity(RPTRequestEntity rptRequestEntity) {
        rptRequestRepository.save(rptRequestEntity);
    }

    private void generateRE(String payload) {

        // creating event to be persisted for RE
        reService.addRe(ReUtil.createBaseReInternal()
                .status(InternalStepStatus.FOUND_RPT_IN_STORAGE)
                .provider(NODO_DEI_PAGAMENTI_SPC)
                .sessionId(MDC.get(Constants.MDC_SESSION_ID))
                .compressedPayload(payload)
                .compressedPayload(String.valueOf(payload != null ? payload.length() : 0))
                .build());
    }
}
