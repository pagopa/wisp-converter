package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
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
        RPTRequestEntity rptRequestEntity = optRPTReqEntity.orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.PERSISTENCE_RPT_NOT_FOUND, sessionId));

        // generate and save RE event internal for change status
        generateRE(rptRequestEntity.getPayload());

        return rptRequestEntity;
    }

    private void generateRE(String payload) {
        // creating event to be persisted for RE
        if (Boolean.TRUE.equals(isTracingOnREEnabled)) {
            ReEventDto reEvent = ReUtil.getREBuilder()
                    .status(InternalStepStatus.RPTS_EXTRACTED)
                    .requestPayload(payload)
                    .requestPayload(String.valueOf(payload != null ? payload.length() : 0))
                    .build();
            reService.addRe(reEvent);
        }
    }
}
