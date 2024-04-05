package it.gov.pagopa.wispconverter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigCacheService {

    private final it.gov.pagopa.gen.wispconverter.client.cache.invoker.ApiClient configCacheClient;

    private it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto configData;


    public it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto getCache() {
        if (configData == null) {
            loadCache();
        }
        return configData;
    }

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
