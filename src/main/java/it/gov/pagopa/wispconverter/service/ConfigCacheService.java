package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@CacheConfig(cacheNames="cache")
@Slf4j
@RequiredArgsConstructor
public class ConfigCacheService {

    private final it.gov.pagopa.gen.wispconverter.client.cache.invoker.ApiClient configCacheClient;

    @Getter
    private it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto configData;

    @Value("${client.cache.keys}")
    private List<String> cacheKeys;

    public void checkCache() {
        if (configData == null) {
            loadCache();
        }
    }

    @Cacheable
    public void loadCache() {
        log.info("loadCache from cache api");
        try {
            it.gov.pagopa.gen.wispconverter.client.cache.api.CacheApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.cache.api.CacheApi(configCacheClient);
            configData = apiInstance.get(cacheKeys);
        } catch (RestClientException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_DECOUPLER_CACHING,
                    String.format("RestClientException ERROR [%s] - %s", e.getCause().getClass().getCanonicalName(), e.getMessage()));
        }
    }

}
