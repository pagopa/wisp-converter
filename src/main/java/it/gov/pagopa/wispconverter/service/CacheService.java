package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private static final String COMPOSITE_TWOVALUES_KEY_TEMPLATE = "%s_%s";

    private static final String CACHING_KEY_TEMPLATE = "wisp_" + COMPOSITE_TWOVALUES_KEY_TEMPLATE;

//    private final DecouplerCachingClient decouplerCachingClient;

    private final CacheRepository cacheRepository;

    @Value("${wisp-converter.cached-requestid-mapping.ttl.minutes}")
    private Long requestIDMappingTTL;


    public void storeRequestMappingInCache(List<RPTContentDTO> rptContentDTOs, String sessionId) {
//        try {
//            rptContentDTOs.forEach(e -> {
//                String idIntermediarioPA = e.getIdIntermediarioPA();
//                String noticeNumber = e.getNoticeNumber();
//
//                String requestIDForDecoupler = String.format(COMPOSITE_TWOVALUES_KEY_TEMPLATE, idIntermediarioPA, noticeNumber); // TODO can be optimized in a single request???
//                this.decouplerCachingClient.storeKeyInCacheByAPIM(requestIDForDecoupler);
//
//                // save in Redis cache the mapping of the request identifier needed for RT generation in next steps
//                String requestIDForRTHandling = String.format(CACHING_KEY_TEMPLATE, idIntermediarioPA, noticeNumber);
//                this.cacheRepository.insert(requestIDForRTHandling, sessionId, this.requestIDMappingTTL);
//            });
//        } catch (FeignException e) {
//            throw new AppException(e, AppErrorCodeMessageEnum.CLIENT_DECOUPLER_CACHING, e.status(), e.getMessage());
//        }
    }
}
