package it.gov.pagopa.wispconverter.service;

import io.lettuce.core.RedisException;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.PaymentNoticeContentDTO;
import it.gov.pagopa.wispconverter.util.Constants;
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
public class CacheService {

    private static final String COMPOSITE_TWOVALUES_KEY_TEMPLATE = "%s_%s";

    private static final String CACHING_KEY_TEMPLATE = "wisp_" + COMPOSITE_TWOVALUES_KEY_TEMPLATE;

    private final it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient decouplerCachingClient;

    private final CacheRepository cacheRepository;

    @Value("${wisp-converter.cached-requestid-mapping.ttl.minutes}")
    private Long requestIDMappingTTL;

    public void storeRequestMappingInCache(CommonRPTFieldsDTO commonRPTFieldsDTO, String sessionId) {
        try {
            String idIntermediarioPA = commonRPTFieldsDTO.getCreditorInstitutionBrokerId();
            List<String> noticeNumbers = commonRPTFieldsDTO.getPaymentNotices().stream()
                    .map(PaymentNoticeContentDTO::getNoticeNumber)
                    .toList();

            // communicating with APIM policy for caching data for decoupler
            it.gov.pagopa.gen.wispconverter.client.decouplercaching.model.DecouplerCachingKeysDto decouplerCachingKeys = new it.gov.pagopa.gen.wispconverter.client.decouplercaching.model.DecouplerCachingKeysDto();
            noticeNumbers.forEach(noticeNumber -> decouplerCachingKeys.addKeysItem(String.format(COMPOSITE_TWOVALUES_KEY_TEMPLATE, idIntermediarioPA, noticeNumber)));
            it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi(decouplerCachingClient);
            apiInstance.saveMapping(decouplerCachingKeys, MDC.get(Constants.MDC_REQUEST_ID));

            // save in Redis cache the mapping of the request identifier needed for RT generation in next steps
            for (String noticeNumber : noticeNumbers) {
                String requestIDForRTHandling = String.format(CACHING_KEY_TEMPLATE, idIntermediarioPA, noticeNumber);
                this.cacheRepository.insert(requestIDForRTHandling, sessionId, this.requestIDMappingTTL);
            }

        } catch (RestClientException e) {
            throw new AppException(e, AppErrorCodeMessageEnum.CLIENT_DECOUPLER_CACHING, e.getMessage());
        } catch (RedisException e) {
            throw new AppException(e, AppErrorCodeMessageEnum.PERSISTENCE_REQUESTID_CACHING_ERROR, e.getMessage());
        }
    }
}
