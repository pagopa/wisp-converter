package it.gov.pagopa.wispconverter.service;

import static it.gov.pagopa.wispconverter.util.Constants.NODO_DEI_PAGAMENTI_SPC;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.service.model.CachedKeysMapping;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.PaymentNoticeContentDTO;
import it.gov.pagopa.wispconverter.service.model.re.EntityStatusEnum;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
@RequiredArgsConstructor
public class DecouplerService {

    private static final String CACHING_KEY_TEMPLATE = "wisp_%s_%s";

    private static final String MAP_CACHING_KEY_TEMPLATE = "wisp_nav2iuv_%s_%s";

    private final ReService reService;

    private final it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient decouplerCachingClient;

    private final CacheRepository cacheRepository;

    @Value("${wisp-converter.cached-requestid-mapping.ttl.minutes}")
    private Long requestIDMappingTTL;

    public void storeRequestMappingInCache(CommonRPTFieldsDTO commonRPTFieldsDTO, String sessionId) {
        try {
            String creditorInstitutionId = commonRPTFieldsDTO.getCreditorInstitutionId();
            List<String> noticeNumbers = commonRPTFieldsDTO.getPaymentNotices().stream()
                    .map(PaymentNoticeContentDTO::getNoticeNumber)
                    .toList();

            // communicating with APIM policy for caching data for decoupler. The stored data are internal to APIM and cannot be retrieved
            it.gov.pagopa.gen.wispconverter.client.decouplercaching.model.DecouplerCachingKeysDto decouplerCachingKeys = new it.gov.pagopa.gen.wispconverter.client.decouplercaching.model.DecouplerCachingKeysDto();
            noticeNumbers.forEach(noticeNumber -> decouplerCachingKeys.addKeysItem(String.format(CACHING_KEY_TEMPLATE, creditorInstitutionId, noticeNumber)));
            it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi(decouplerCachingClient);
            apiInstance.saveMapping(decouplerCachingKeys, MDC.get(Constants.MDC_REQUEST_ID));

            // save in Redis cache the mapping of the request identifier needed for RT generation in next steps
            for (PaymentNoticeContentDTO paymentNoticeContentDTO : commonRPTFieldsDTO.getPaymentNotices()) {
                // save the IUV-based key that contains the session identifier
                String requestIDForRTHandling = String.format(CACHING_KEY_TEMPLATE, creditorInstitutionId, paymentNoticeContentDTO.getNoticeNumber());
                this.cacheRepository.insert(requestIDForRTHandling, sessionId, this.requestIDMappingTTL);
                // save the mapping that permits to convert a NAV-based key in a IUV-based one
                String navToIuvMappingForRTHandling = String.format(MAP_CACHING_KEY_TEMPLATE, creditorInstitutionId, paymentNoticeContentDTO.getNoticeNumber());
                this.cacheRepository.insert(navToIuvMappingForRTHandling, requestIDForRTHandling, this.requestIDMappingTTL);
            }

            // generate and save re events internal for change status
            reService.addRe(generateRE());

        } catch (RestClientException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_DECOUPLER_CACHING, String.format("RestClientException ERROR [%s] - %s", e.getCause().getClass().getCanonicalName(), e.getMessage()));
        }
    }

    public String getCachedSessionId(String creditorInstitutionId, String iuv) {
        String cachedKey = String.format(CACHING_KEY_TEMPLATE, creditorInstitutionId, iuv);
        String sessionId = this.cacheRepository.read(cachedKey, String.class);
        if (sessionId == null) {
            throw new AppException(AppErrorCodeMessageEnum.PERSISTENCE_REQUESTID_CACHING_ERROR, cachedKey);
        }
        return sessionId;
    }

    public CachedKeysMapping getCachedMappingFromNavToIuv(String creditorInstitutionId, String nav) {
        String mappingKey = String.format(MAP_CACHING_KEY_TEMPLATE, creditorInstitutionId, nav);
        String keyWithIUV = this.cacheRepository.read(mappingKey, String.class);
        if (keyWithIUV == null) {
            throw new AppException(AppErrorCodeMessageEnum.PERSISTENCE_REQUESTID_CACHING_ERROR, mappingKey);
        }
        String[] splitKey = keyWithIUV.split("_");
        if (splitKey.length != 3) {
            throw new AppException(AppErrorCodeMessageEnum.PERSISTENCE_MAPPING_NAV_TO_IUV_ERROR, mappingKey);
        }
        return CachedKeysMapping.builder()
                .fiscalCode(splitKey[1])
                .iuv(splitKey[2])
                .build();
    }

    private ReEventDto generateRE() {
        return ReUtil.createBaseReInternal()
                .status(EntityStatusEnum.RPT_CACHE_PER_DECOUPLER_GENERATA.name())
                .erogatore(NODO_DEI_PAGAMENTI_SPC)
                .erogatoreDescr(NODO_DEI_PAGAMENTI_SPC)
                .sessionIdOriginal(MDC.get(Constants.MDC_SESSION_ID))
                .tipoEvento(MDC.get(Constants.MDC_EVENT_TYPE))
                .cartId(MDC.get(Constants.MDC_CART_ID))
                .idDominio(MDC.get(Constants.MDC_DOMAIN_ID))
                .stazione(MDC.get(Constants.MDC_STATION_ID))
                .iuv(MDC.get(Constants.MDC_IUV)) // null if nodoInviaCarrelloRPT
                .noticeNumber(MDC.get(Constants.MDC_NOTICE_NUMBER)) // null if nodoInviaCarrelloRPT
                .build();
    }
}
