package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.gen.wispconverter.client.decouplercaching.model.DecouplerCachingKeysDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.service.model.CachedKeysMapping;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.service.model.session.PaymentNoticeContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DecouplerService {

    public static final String CACHING_KEY_TEMPLATE = "wisp_%s_%s";
    private static final String CARTSESSION_CACHING_KEY_TEMPLATE = "%s_%s_%s";

    public static final String MAP_CACHING_KEY_TEMPLATE = "wisp_nav2iuv_%s_%s";

    private final ReService reService;

    private final it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient decouplerCachingClient;

    private final CacheRepository cacheRepository;

    @Value("${wisp-converter.cached-requestid-mapping.ttl.minutes}")
    private Long requestIDMappingTTL;

    @Value("${wisp-converter.re-tracing.internal.decoupler-caching.enabled}")
    private Boolean isTracingOnREEnabled;

    public void storeRequestMappingInCache(SessionDataDTO sessionData, String sessionId) {

        try {

            // call APIM endpoint (formed only by a policy) in order to store mapped NAVs in APIM-internal cache
            saveMappedKeyForDecoupler(sessionData);

            // save in Redis cache (accessible for this app) the mapping of the request identifier needed for RT generation in next steps
            saveMappedKeyForReceiptGeneration(sessionId, sessionData);

        } catch (RestClientException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_DECOUPLER_CACHING, String.format("RestClientException ERROR [%s] - %s", e.getCause().getClass().getCanonicalName(), e.getMessage()));
        }
    }

    public void storeRequestCartMappingInCache(SessionDataDTO sessionData, String sessionId) {

        try {

            // generate client instance for APIM endpoint
            it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi(decouplerCachingClient);

            /*
              Communicating with APIM policy for caching data for cart session.
              The stored data are internal to APIM and cannot be retrieved from this app.
             */
            DecouplerCachingKeysDto cartSessionCachingKeys = new DecouplerCachingKeysDto();
            List<String> noticeNumbers = sessionData.getNAVs();
            for (String noticeNumber : noticeNumbers) {
                PaymentNoticeContentDTO paymentNotice = sessionData.getPaymentNoticeByNoticeNumber(noticeNumber);
                cartSessionCachingKeys.addKeysItem(String.format(CARTSESSION_CACHING_KEY_TEMPLATE, sessionId, paymentNotice.getFiscalCode(), noticeNumber));
            }
            apiInstance.saveCartMapping(cartSessionCachingKeys, MDC.get(Constants.MDC_REQUEST_ID));

            // generate and save re events internal for change status
            generateREForSavedMappingForCartSessionMapping(sessionData, cartSessionCachingKeys);

        } catch (RestClientException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_CARTSESSION_CACHING, String.format("RestClientException ERROR [%s] - %s", e.getCause().getClass().getCanonicalName(), e.getMessage()));
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

        // retrieving mapped key from Redis cache
        String keyWithIUV = this.cacheRepository.read(mappingKey, String.class);
        if (keyWithIUV == null) {
            throw new AppException(AppErrorCodeMessageEnum.PERSISTENCE_REQUESTID_CACHING_ERROR, mappingKey);
        }

        // trying to split key on underscore character
        String[] splitKey = keyWithIUV.split("_");
        if (splitKey.length != 3) {
            throw new AppException(AppErrorCodeMessageEnum.PERSISTENCE_MAPPING_NAV_TO_IUV_ERROR, mappingKey);
        }

        // returning the key, correctly split
        return CachedKeysMapping.builder()
                .fiscalCode(splitKey[1])
                .iuv(splitKey[2])
                .build();
    }


    //TODO duplicate method but pass only sessionId recovered from service bus
    private void saveMappedKeyForDecoupler(SessionDataDTO sessionData) {

        // generate client instance for APIM endpoint
        it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi(decouplerCachingClient);

        /*
          Communicating with APIM policy for caching data for decoupler.
          The stored data are internal to APIM and cannot be retrieved from this app.
         */
        DecouplerCachingKeysDto decouplerCachingKeys = new DecouplerCachingKeysDto();

        List<String> noticeNumbers = sessionData.getNAVs();
        for (String noticeNumber : noticeNumbers) {
            PaymentNoticeContentDTO paymentNotice = sessionData.getPaymentNoticeByNoticeNumber(noticeNumber);
            decouplerCachingKeys.addKeysItem(String.format(CACHING_KEY_TEMPLATE, paymentNotice.getFiscalCode(), noticeNumber));
        }
        apiInstance.saveMapping(decouplerCachingKeys, MDC.get(Constants.MDC_REQUEST_ID));

        // generate and save re events internal for change status
        generateREForSavedMappingForDecoupler(sessionData, decouplerCachingKeys);
    }

    public void saveMappedKeyForReceiptGeneration(String sessionId, SessionDataDTO sessionData) {

        for (PaymentNoticeContentDTO paymentNoticeContentDTO : sessionData.getAllPaymentNotices()) {

            // save the IUV-based key that contains the session identifier
            String requestIDForRTHandling = String.format(CACHING_KEY_TEMPLATE, paymentNoticeContentDTO.getFiscalCode(), paymentNoticeContentDTO.getIuv());
            this.cacheRepository.insert(requestIDForRTHandling, sessionId, this.requestIDMappingTTL);

            // save the mapping that permits to convert a NAV-based key in a IUV-based one
            String navToIuvMappingForRTHandling = String.format(MAP_CACHING_KEY_TEMPLATE, paymentNoticeContentDTO.getFiscalCode(), paymentNoticeContentDTO.getNoticeNumber());
            this.cacheRepository.insert(navToIuvMappingForRTHandling, requestIDForRTHandling, this.requestIDMappingTTL);

            // generate and save re events internal for change status
            String infoAboutCachedKey = "Main key = [(key:" + requestIDForRTHandling + "; value:" + sessionId + ")], Mapping key = [(key:" + navToIuvMappingForRTHandling + "; value:" + requestIDForRTHandling + ")]";
            generateREForSavedMappingForRTGeneration(paymentNoticeContentDTO, infoAboutCachedKey);
        }
    }

    private void generateREForSavedMappingForDecoupler(SessionDataDTO sessionData, DecouplerCachingKeysDto decouplerCachingKeys) {

        // creating event to be persisted for RE
        if (Boolean.TRUE.equals(isTracingOnREEnabled)) {
            for (String key : decouplerCachingKeys.getKeys()) {
                String[] splitKey = key.split("_");
                if (splitKey.length == 3) {
                    PaymentNoticeContentDTO paymentNotice = sessionData.getPaymentNoticeByNoticeNumber(splitKey[2]);
                    String infoAboutCachedKey = "Decoupler key = [(key:" + key + "; value:<baseNodeId>)]";
                    generateRE(InternalStepStatus.GENERATED_CACHE_ABOUT_RPT_FOR_DECOUPLER, paymentNotice.getFiscalCode(), paymentNotice.getIuv(), paymentNotice.getNoticeNumber(), paymentNotice.getCcp(), infoAboutCachedKey);
                }
            }
        }
    }

    private void generateREForSavedMappingForCartSessionMapping(SessionDataDTO sessionData, DecouplerCachingKeysDto cartCachingKeys) {

        // creating event to be persisted for RE
        if (Boolean.TRUE.equals(isTracingOnREEnabled)) {
            for (String key : cartCachingKeys.getKeys()) {
                String[] splitKey = key.split("_");
                if (splitKey.length == 3) {
                    PaymentNoticeContentDTO paymentNotice = sessionData.getPaymentNoticeByNoticeNumber(splitKey[2]);
                    String infoAboutCachedKey = "Cart mapping key = [(key:" + key + "; value:<baseNodeId>)]";
                    generateRE(InternalStepStatus.GENERATED_CACHE_ABOUT_RPT_FOR_CARTSESSION_CACHING, paymentNotice.getFiscalCode(), paymentNotice.getIuv(), paymentNotice.getNoticeNumber(), paymentNotice.getCcp(), infoAboutCachedKey);
                }
            }
        }
    }

    private void generateREForSavedMappingForRTGeneration(PaymentNoticeContentDTO paymentNotice, String infoAboutCachedKey) {

        // creating event to be persisted for RE
        if (Boolean.TRUE.equals(isTracingOnREEnabled)) {
            generateRE(InternalStepStatus.GENERATED_CACHE_ABOUT_RPT_FOR_RT_GENERATION, paymentNotice.getFiscalCode(), paymentNotice.getIuv(), paymentNotice.getNoticeNumber(), paymentNotice.getCcp(), infoAboutCachedKey);
        }
    }

    private void generateRE(InternalStepStatus status, String domainId, String iuv, String nav, String ccp, String otherInfo) {

        ReEventDto reEvent = ReUtil.getREBuilder()
                .status(status)
                .domainId(domainId)
                .iuv(iuv)
                .noticeNumber(nav)
                .ccp(ccp)
                .info(otherInfo)
                .build();
        reService.addRe(reEvent);
    }
}
