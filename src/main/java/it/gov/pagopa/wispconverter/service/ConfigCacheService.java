package it.gov.pagopa.wispconverter.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Getter
@CacheConfig(cacheNames="cache")
@Slf4j
@RequiredArgsConstructor
public class ConfigCacheService {

    private final it.gov.pagopa.gen.wispconverter.client.cache.invoker.ApiClient configCacheClient;

    private it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto configData;

    public void getCache() {
        loadCache();
    }

    @Cacheable
    public void loadCache() {
        log.info("loadCache from cache api");
        try {
            it.gov.pagopa.gen.wispconverter.client.cache.api.CacheApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.cache.api.CacheApi(configCacheClient);
            configData = apiInstance.cache(false);
        } catch (Exception e) {
            log.error("Cannot get cache", e);
        }
    }

}
