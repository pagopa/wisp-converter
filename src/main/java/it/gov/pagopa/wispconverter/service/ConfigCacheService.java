package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.client.cache.api.CacheApi;
import it.gov.pagopa.wispconverter.client.cache.invoker.ApiClient;
import it.gov.pagopa.wispconverter.client.cache.model.ConfigDataV1Dto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigCacheService {

    private final ApiClient configCacheClient;

    private ConfigDataV1Dto configData;


    public ConfigDataV1Dto getCache() {
        if (configData == null) {
            loadCache();
        }
        return configData;
    }

    public void loadCache() {
        log.info("loadCache from cache api");
        try {
            CacheApi apiInstance = new CacheApi(configCacheClient);
            configData = apiInstance.cache();
        } catch (Exception e) {
            log.error("Cannot get cache", e);
        }
    }
}
