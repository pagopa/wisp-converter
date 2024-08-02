package it.gov.pagopa.wispconverter.service;

import com.azure.cosmos.models.PartitionKey;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RTRetryRepository;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RtRetryComosService {

    private final ReService reService;

    private final RTRetryRepository rtRetryRepository;

    @Value("${wisp-converter.re-tracing.internal.rt-retrieving.enabled}")
    private Boolean isTracingOnREEnabled;

    @Transactional
    public void saveRTRequestEntity(RTRequestEntity rtRequestEntity) {
        rtRetryRepository.save(rtRequestEntity);
    }

    @Transactional
    public void deleteRTRequestEntity(RTRequestEntity rtRequestEntity) {
        rtRetryRepository.delete(rtRequestEntity);
    }

    public RTRequestEntity getRTRequestEntity(String id, String partitionKey) {

        // searching RT by id and partition key: if no element is found throw an exception, in the RE will be saved an exception event of failure
        Optional<RTRequestEntity> optRTReqEntity = rtRetryRepository.findById(id, new PartitionKey(partitionKey));
        RTRequestEntity rtRequestEntity = optRTReqEntity.orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.PERSISTENCE_RT_NOT_FOUND, id, partitionKey));

        // generate and save RE event internal for change status
        generateRE(rtRequestEntity.getPayload());

        return rtRequestEntity;
    }

    private void generateRE(String payload) {

        // creating event to be persisted for RE
        if (Boolean.TRUE.equals(isTracingOnREEnabled)) {
            ReEventDto reEvent = ReUtil.getREBuilder()
                    .status(InternalStepStatus.FOUND_RT_IN_STORAGE)
                    .compressedPayload(payload)
                    .compressedPayload(String.valueOf(payload != null ? payload.length() : 0))
                    .build();
            reService.addRe(reEvent);
        }
    }
}
