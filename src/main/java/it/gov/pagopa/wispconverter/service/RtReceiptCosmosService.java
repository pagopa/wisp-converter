package it.gov.pagopa.wispconverter.service;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.PartitionKey;
import it.gov.pagopa.wispconverter.repository.RTRepository;
import it.gov.pagopa.wispconverter.repository.model.RTEntity;
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


@Service
@Slf4j
@RequiredArgsConstructor
public class RtReceiptCosmosService {

    private final RTRepository rtRepository;
    private static final String ILLEGAL_CHARS_FOR_ID = "[/\\\\#]";

    @Transactional
    public void saveRTEntity(SessionDataDTO sessionData) {
        for(RPTContentDTO rptContentDTO : sessionData.getAllRPTs()) {
            try {
                rtRepository.save(createRTEntity(rptContentDTO, null, null, null));
            } catch (CosmosException e) {
                log.error("An exception occurred while saveRTEntity: " + e.getMessage());
            }
        }
    }

    @Transactional
    public void saveRTEntity(RPTContentDTO rptContentDTO, String rawRt, ReceiptTypeEnum receiptType) {
        try {
            String encodedRt = AppBase64Util.base64Encode(ZipUtil.zip(rawRt));
            rtRepository.save(createRTEntity(rptContentDTO, encodedRt, receiptType, ZonedDateTime.now().toInstant().toEpochMilli()));
        } catch (IOException | CosmosException e) {
            log.error("An exception occurred while saveRTEntity: " + e.getMessage());
        }
    }

    public boolean receiptRtExist(String domainId, String iuv, String ccp) {
        String id = String.format("%s_%s_%s", domainId, iuv, ccp);
        // Remove illegal characters ['/', '\', '#'] because cannot be used in Resource ID
        id = id.replaceAll(ILLEGAL_CHARS_FOR_ID, "");
        return rtRepository.findById(id, new PartitionKey(id)).isPresent();
    }

    private RTEntity createRTEntity(RPTContentDTO rptContentDTO, String rt, ReceiptTypeEnum receiptType, Long rtTimestamp) {
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
                .idDominio(domainId)
                .rt(rt)
                .receiptType(receiptType)
                .rtTimestamp(rtTimestamp)
                .build();
    }
}
