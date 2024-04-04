package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.client.decouplercaching.DecouplerCachingClient;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private static final String COMPOSITE_TWOVALUES_KEY_TEMPLATE = "%s_%s";

    private static final String CACHING_KEY_TEMPLATE = "wisp_" + COMPOSITE_TWOVALUES_KEY_TEMPLATE;

    private final DecouplerCachingClient decouplerCachingClient;

    private final CacheRepository cacheRepository;

    @Value("${wisp-converter.cached-requestid-mapping.ttl.minutes}")
    private Long requestIDMappingTTL;


    public void storeRequestMappingInCache(List<RPTContentDTO> rptContentDTOs, String sessionId) {
        rptContentDTOs.forEach(e -> {
            String idIntermediarioPA = e.getIdIntermediarioPA();
            String noticeNumber = e.getNoticeNumber();
            String requestIDForDecoupler = String.format(COMPOSITE_TWOVALUES_KEY_TEMPLATE, idIntermediarioPA, noticeNumber); // TODO can be optimized in a single request???

            it.gov.pagopa.decouplercachingclient.model.DecouplerCachingKeysDto decouplerCachingKeysDto = new it.gov.pagopa.decouplercachingclient.model.DecouplerCachingKeysDto();
            decouplerCachingKeysDto.setKeys(Collections.singletonList(requestIDForDecoupler));

            String xRequestId = MDC.get(Constants.MDC_REQUEST_ID);

            it.gov.pagopa.decouplercachingclient.api.DefaultApi apiInstance = new it.gov.pagopa.decouplercachingclient.api.DefaultApi(decouplerCachingClient);
            apiInstance.saveMapping(decouplerCachingKeysDto, xRequestId);

            // save in Redis cache the mapping of the request identifier needed for RT generation in next steps
            String requestIDForRTHandling = String.format(CACHING_KEY_TEMPLATE, idIntermediarioPA, noticeNumber);
            this.cacheRepository.insert(requestIDForRTHandling, sessionId, this.requestIDMappingTTL);
        });
    }
}
