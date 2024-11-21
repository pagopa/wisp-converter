package it.gov.pagopa.wispconverter.service;

import com.azure.cosmos.models.PartitionKey;
import it.gov.pagopa.gen.wispconverter.client.decouplercaching.model.DecouplerCachingKeysDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.NavToIuvMappingRepository;
import it.gov.pagopa.wispconverter.repository.model.NavToIuvMappingEntity;
import it.gov.pagopa.wispconverter.service.model.CachedKeysMapping;
import it.gov.pagopa.wispconverter.service.model.session.PaymentNoticeContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import it.gov.pagopa.wispconverter.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DecouplerService {

    public static final String CACHING_KEY_TEMPLATE = "wisp_%s_%s";
    public static final String MAP_CACHING_KEY_TEMPLATE = "wisp_nav2iuv_%s_%s";
    private static final String CARTSESSION_CACHING_KEY_TEMPLATE = "%s_%s_%s";
    private final ReService reService;

    private final it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient
            decouplerCachingClient;

    private final CacheRepository cacheRepository;

    private final NavToIuvMappingRepository navToIuvMappingRepository;

    @Value("${wisp-converter.cached-requestid-mapping.ttl.minutes}")
    private Long requestIDMappingTTL;

    @Value("${wisp-converter.re-tracing.internal.decoupler-caching.enabled}")
    private Boolean isTracingOnREEnabled;

    public void storeRequestMappingInCache(SessionDataDTO sessionData) {

        try {

            // call APIM endpoint (formed only by a policy) in order to store mapped NAVs in APIM-internal
            // cache
            saveMappedKeyForDecoupler(sessionData);

            // save in Redis cache (accessible for this app) the mapping of the request identifier needed
            // for RT generation in next steps
            saveMappedKeyForReceiptGeneration(sessionData);

        } catch (RestClientException e) {
            throw new AppException(
                    AppErrorCodeMessageEnum.CLIENT_DECOUPLER_CACHING,
                    String.format(
                            "RestClientException ERROR [%s] - %s",
                            e.getCause().getClass().getCanonicalName(), e.getMessage()));
        }
    }

    public void storeRequestCartMappingInCache(SessionDataDTO sessionData, String sessionId) {

        try {

            // generate client instance for APIM endpoint
            it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi apiInstance =
                    new it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi(
                            decouplerCachingClient);

      /*
       Communicating with APIM policy for caching data for cart session.
       The stored data are internal to APIM and cannot be retrieved from this app.
      */
            DecouplerCachingKeysDto cartSessionCachingKeys = new DecouplerCachingKeysDto();
            List<String> noticeNumbers = sessionData.getNAVs();
            for (String noticeNumber : noticeNumbers) {
                PaymentNoticeContentDTO paymentNotice =
                        sessionData.getPaymentNoticeByNoticeNumber(noticeNumber);
                cartSessionCachingKeys.addKeysItem(
                        String.format(
                                CARTSESSION_CACHING_KEY_TEMPLATE,
                                sessionId,
                                paymentNotice.getFiscalCode(),
                                noticeNumber));
            }
            apiInstance.saveCartMapping(cartSessionCachingKeys, MDC.get(Constants.MDC_REQUEST_ID));

        } catch (RestClientException e) {
            throw new AppException(
                    AppErrorCodeMessageEnum.CLIENT_CARTSESSION_CACHING,
                    String.format(
                            "RestClientException ERROR [%s] - %s",
                            e.getCause().getClass().getCanonicalName(), e.getMessage()));
        }
    }

    public CachedKeysMapping getCachedMappingFromNavToIuv(String creditorInstitutionId, String nav) {

        String mappingKey = String.format(MAP_CACHING_KEY_TEMPLATE, creditorInstitutionId, nav);
        String cachedFiscalCode;
        String cachedIUV;

        // retrieving mapped key from Redis cache
        String keyWithIUV = this.cacheRepository.read(mappingKey, String.class);
        if (keyWithIUV != null) {

            // trying to split key on underscore character
            String[] splitKey = keyWithIUV.split("_", 3);
            if (splitKey.length < 3) {
                throw new AppException(
                        AppErrorCodeMessageEnum.PERSISTENCE_MAPPING_NAV_TO_IUV_ERROR, mappingKey);
            }
            cachedFiscalCode = splitKey[1];
            cachedIUV = splitKey[2];

        } else {

            // if no mapping is found, retrieve it from DB and try to bypass short TTL in Redis
            Optional<NavToIuvMappingEntity> optNavToIuvMapping =
                    this.navToIuvMappingRepository.findById(nav, new PartitionKey(creditorInstitutionId));
            if (optNavToIuvMapping.isEmpty()) {
                throw new AppException(
                        AppErrorCodeMessageEnum.PERSISTENCE_REQUESTID_CACHING_ERROR, mappingKey);
            }
            NavToIuvMappingEntity navToIuvMappingEntity = optNavToIuvMapping.get();
            cachedFiscalCode = navToIuvMappingEntity.getPartitionKey();
            cachedIUV = navToIuvMappingEntity.getIuv();
        }

        // returning the key, correctly extracted
        return CachedKeysMapping.builder().fiscalCode(cachedFiscalCode).iuv(cachedIUV).build();
    }

    private void saveMappedKeyForDecoupler(SessionDataDTO sessionData) {

        // generate client instance for APIM endpoint
        it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi apiInstance =
                new it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi(
                        decouplerCachingClient);

    /*
     Communicating with APIM policy for caching data for decoupler.
     The stored data are internal to APIM and cannot be retrieved from this app.
    */
        DecouplerCachingKeysDto decouplerCachingKeys = new DecouplerCachingKeysDto();

        List<String> noticeNumbers = sessionData.getNAVs();
        for (String noticeNumber : noticeNumbers) {
            PaymentNoticeContentDTO paymentNotice =
                    sessionData.getPaymentNoticeByNoticeNumber(noticeNumber);
            decouplerCachingKeys.addKeysItem(
                    String.format(CACHING_KEY_TEMPLATE, paymentNotice.getFiscalCode(), noticeNumber));
        }
        apiInstance.saveMapping(decouplerCachingKeys, MDC.get(Constants.MDC_OPERATION_ID));
    }

    /**
     * this method creates 2 mapping: wisp_nav2iuv_<fiscal-code>_<nav> = wisp_<fiscal-code>_<iuv>
     *
     * @param sessionData the data of the session
     */
    public void saveMappedKeyForReceiptGeneration(SessionDataDTO sessionData) {

        for (PaymentNoticeContentDTO paymentNoticeContentDTO : sessionData.getAllPaymentNotices()) {

            // save the IUV-based key that contains the session identifier
            String requestIDForRTHandling =
                    String.format(
                            CACHING_KEY_TEMPLATE,
                            paymentNoticeContentDTO.getFiscalCode(),
                            paymentNoticeContentDTO.getIuv());

            // save the mapping that permits to convert a NAV-based key in a IUV-based one
            String navToIuvMappingForRTHandling =
                    String.format(
                            MAP_CACHING_KEY_TEMPLATE,
                            paymentNoticeContentDTO.getFiscalCode(),
                            paymentNoticeContentDTO.getNoticeNumber());
            this.cacheRepository.insert(
                    navToIuvMappingForRTHandling, requestIDForRTHandling, this.requestIDMappingTTL);

            // save the previous mapping in DB as long-duration data
            NavToIuvMappingEntity navToIuvMappingEntity =
                    NavToIuvMappingEntity.builder()
                            .id(paymentNoticeContentDTO.getNoticeNumber())
                            .partitionKey(paymentNoticeContentDTO.getFiscalCode())
                            .iuv(paymentNoticeContentDTO.getIuv())
                            .build();
            this.navToIuvMappingRepository.save(navToIuvMappingEntity);
        }
    }

}
