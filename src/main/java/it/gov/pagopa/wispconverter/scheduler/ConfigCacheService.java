package it.gov.pagopa.wispconverter.scheduler;

import it.gov.pagopa.wispconverter.client.cache.ApiClient;
import it.gov.pagopa.wispconverter.client.cache.model.ConfigDataV1;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.client.api.CacheApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConfigCacheService {

    private final CacheApi cacheClient;

    private ConfigDataV1 configData;

    public ConfigCacheService(@Value("${client.cache.host}") String basePath,
                              @Value("${client.cache.subscription-key}") String apiKey) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(basePath);
        apiClient.setApiKey(apiKey);
        this.cacheClient = new CacheApi(apiClient);
    }

    public ConfigDataV1 getCache() {
        if (configData == null) {
            loadCache();
        }
        return configData;
    }

    public void loadCache() {
        log.info("loadCache from cache api");
        try {
            configData = cacheClient.cache();
        } catch (Exception e) {
            log.error("Cannot get cache", e);
        }
    }
}
