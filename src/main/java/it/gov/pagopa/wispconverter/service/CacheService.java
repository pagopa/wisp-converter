package it.gov.pagopa.wispconverter.service;

import feign.FeignException;
import io.lettuce.core.RedisException;
import it.gov.pagopa.wispconverter.client.DecouplerCachingClient;
import it.gov.pagopa.wispconverter.exception.conversion.ConversionException;
import it.gov.pagopa.wispconverter.model.client.gpd.MultiplePaymentPosition;
import it.gov.pagopa.wispconverter.model.client.gpd.PaymentOption;
import it.gov.pagopa.wispconverter.model.client.gpd.PaymentPosition;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CacheService {

    private static final String COMPOSITE_TWOVALUES_KEY_TEMPLATE = "%s_%s";

    private static final String CACHING_KEY_TEMPLATE = "wisp_" + COMPOSITE_TWOVALUES_KEY_TEMPLATE;

    private final DecouplerCachingClient decouplerCachingClient;

    private final CacheRepository cacheRepository;

    private final Long requestIDMappingTTL;

    public CacheService(@Autowired DecouplerCachingClient decouplerCachingClient,
                        @Autowired CacheRepository cacheRepository,
                        @Value("${wisp-converter.cached-requestid-mapping.ttl.minutes}") Long requestIDMappingTTL) {
        this.decouplerCachingClient = decouplerCachingClient;
        this.cacheRepository = cacheRepository;
        this.requestIDMappingTTL = requestIDMappingTTL;
    }

    public void storeRequestMappingInCache(String idIntermediarioPA, MultiplePaymentPosition paymentPositions, String sessionId) throws ConversionException {
        try {

            // extracting all NAV codes from all payment options
            Set<String> navCodes = paymentPositions.getPaymentPositions().stream()
                    .map(PaymentPosition::getPaymentOption)
                    .flatMap(List::stream)
                    .map(PaymentOption::getNav)
                    .collect(Collectors.toSet());

            for (String nav : navCodes) {

                // communicating with APIM policy in order to save the request key needed for decoupler
                String requestIDForDecoupler = String.format(COMPOSITE_TWOVALUES_KEY_TEMPLATE, idIntermediarioPA, nav); // TODO can be optimized in a single request???
                this.decouplerCachingClient.storeKeyInCacheByAPIM(requestIDForDecoupler);

                // save in Redis cache the mapping of the request identifier needed for RT generation in next steps
                String requestIDForRTHandling = String.format(CACHING_KEY_TEMPLATE, idIntermediarioPA, nav);
                this.cacheRepository.insert(requestIDForRTHandling, sessionId, this.requestIDMappingTTL);
            }

        } catch (FeignException e) {
            throw new ConversionException("Unable to store request mappings in cache. An error occurred during communication with dedicated APIM:", e);
        } catch (RedisException e) {
            throw new ConversionException("Unable to store request mappings in cache. An error occurred during storage operation in Redis cache:", e);
        }
    }
}
