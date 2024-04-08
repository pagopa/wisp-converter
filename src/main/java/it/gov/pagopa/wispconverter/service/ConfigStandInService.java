package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.gen.wispconverter.client.standin.invoker.ApiClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Getter
@CacheConfig(cacheNames="standin")
@Slf4j
@RequiredArgsConstructor
public class ConfigStandInService {

    private final it.gov.pagopa.gen.wispconverter.client.standin.invoker.ApiClient standInClient;

    private it.gov.pagopa.gen.wispconverter.client.standin.model.ConfigDataV1Dto configData;

    public void getCache() {
        loadStandInCache();
    }

    @Cacheable
    public void loadStandInCache() {
        log.info("loadStandInCache from cache api");
        try {
            it.gov.pagopa.gen.wispconverter.client.standin.api.CacheApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.standin.api.CacheApi(standInClient);
            configData = apiInstance.cache(false);
        } catch (Exception e) {
            log.error("Cannot get standIn cache", e);
        }
    }




}
