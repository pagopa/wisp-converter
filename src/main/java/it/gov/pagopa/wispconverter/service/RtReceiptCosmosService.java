package it.gov.pagopa.wispconverter.service;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.PartitionKey;
import it.gov.pagopa.wispconverter.repository.RTRepository;
import it.gov.pagopa.wispconverter.repository.model.RTEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptStatusEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptTypeEnum;
import it.gov.pagopa.wispconverter.service.model.session.RPTContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import it.gov.pagopa.wispconverter.util.AppBase64Util;
import it.gov.pagopa.wispconverter.util.ZipUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class RtReceiptCosmosService {

    private final RTRepository rtRepository;
    private static final String ILLEGAL_CHARS_FOR_ID = "[/\\\\#]";

    @Transactional
    public void saveRTEntity(SessionDataDTO sessionData, ReceiptStatusEnum status) {
        String sessionId = sessionData.getCommonFields().getSessionId();
        for(RPTContentDTO rptContentDTO : sessionData.getAllRPTs()) {
            try {
                rtRepository.save(createRTEntity(sessionId, rptContentDTO, status,null, null, null));
            } catch (CosmosException e) {
                log.error("An exception occurred while saveRTEntity: " + e.getMessage());
            }
        }
    }

    @Transactional
    public void saveRTEntity(String sessionId, RPTContentDTO rptContentDTO, ReceiptStatusEnum status, String rawRt, ReceiptTypeEnum receiptType) {
        try {
            String encodedRt = AppBase64Util.base64Encode(ZipUtil.zip(rawRt));
            rtRepository.save(createRTEntity(sessionId, rptContentDTO, status, encodedRt, receiptType, ZonedDateTime.now().toInstant().toEpochMilli()));
        } catch (IOException | CosmosException e) {
            log.error("An exception occurred while saveRTEntity: " + e.getMessage());
        }
    }

    @Transactional
    public boolean updateReceiptStatus(RPTContentDTO rptContentDTO, ReceiptStatusEnum status) {
        try {
            String domainId = rptContentDTO.getRpt().getDomain().getDomainId();
            String iuv = rptContentDTO.getIuv();
            String ccp = rptContentDTO.getCcp();

            String id = String.format("%s_%s_%s", domainId, iuv, ccp);
            Optional<RTEntity> rtEntityOptional = rtRepository.findById(id, new PartitionKey(id));

            if(rtEntityOptional.isEmpty()) return false;

            RTEntity old = rtEntityOptional.get();
            RTEntity newRtEntity = createRTEntity(old.getSessionId(), rptContentDTO, status, old.getRt(), old.getReceiptType(), old.getRtTimestamp());
            rtRepository.save(newRtEntity);
        } catch (CosmosException e) {
            log.error("An exception occurred while saveRTEntity: " + e.getMessage());
        }
    }


    public boolean receiptRtExist(String domainId, String iuv, String ccp) {
        String id = String.format("%s_%s_%s", domainId, iuv, ccp);
        // Remove illegal characters ['/', '\', '#'] because cannot be used in Resource ID
        id = id.replaceAll(ILLEGAL_CHARS_FOR_ID, "");
        return rtRepository.findById(id, new PartitionKey(id)).isPresent();
    }

    private RTEntity createRTEntity(String sessionId, RPTContentDTO rptContentDTO, ReceiptStatusEnum status, String rt, ReceiptTypeEnum receiptType, Long rtTimestamp) {
        String domainId = rptContentDTO.getRpt().getDomain().getDomainId();
        String iuv = rptContentDTO.getIuv();
        String ccp = rptContentDTO.getCcp();

        String id = String.format("%s_%s_%s", domainId, iuv, ccp);

        // Remove illegal characters ['/', '\', '#'] because cannot be used in Resource ID
        id = id.replaceAll(ILLEGAL_CHARS_FOR_ID, "");

        return RTEntity.builder()
                .id(id)
                .partitionKey(id)
                .iuv(iuv)
                .ccp(ccp)
                .sessionId(sessionId)
                .receiptStatus(status)
                .idDominio(domainId)
                .rt(rt)
                .receiptType(receiptType)
                .rtTimestamp(rtTimestamp)
                .build();
    }
}
